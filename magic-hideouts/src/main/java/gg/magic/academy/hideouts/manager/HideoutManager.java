package gg.magic.academy.hideouts.manager;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.hideouts.module.HideoutModule;
import gg.magic.academy.hideouts.module.ModuleRegistry;
import gg.magic.academy.hideouts.module.ModuleTier;
import gg.magic.academy.items.registry.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages player hideout worlds and module upgrades.
 * Each player gets a personal world named "hideout_<uuid_short>".
 * Hideout worlds are lazy-loaded on visit and unloaded after idle timeout.
 */
public class HideoutManager implements Listener {

    private static final long IDLE_UNLOAD_TICKS = 12000L; // 10 minutes

    private final JavaPlugin plugin;
    private final ModuleRegistry moduleRegistry;

    /** playerUUID -> world name (persisted in DB via a simple lookup) */
    private final Map<UUID, String> worldNames = new HashMap<>();

    /** World name -> last activity timestamp */
    private final Map<String, Long> worldActivity = new HashMap<>();

    public HideoutManager(JavaPlugin plugin, ModuleRegistry moduleRegistry) {
        this.plugin = plugin;
        this.moduleRegistry = moduleRegistry;
        startIdleUnloadTask();
    }

    public void visitHideout(Player player) {
        String worldName = getOrCreateWorldName(player.getUniqueId());
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            // Load the world
            world = plugin.getServer().createWorld(
                    WorldCreator.name(worldName)
                            .type(WorldType.FLAT)
                            .generateStructures(false)
            );
        }

        if (world == null) {
            player.sendMessage(Component.text("✦ Failed to load your hideout. Contact staff.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        worldActivity.put(worldName, System.currentTimeMillis());
        player.teleport(world.getSpawnLocation());
        player.sendMessage(Component.text("✦ Welcome to your hideout!")
                .color(TextColor.color(0xAAFF55)));
    }

    public boolean upgradeModule(Player player, String moduleId) {
        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return false;

        Optional<HideoutModule> moduleOpt = moduleRegistry.get(moduleId);
        if (moduleOpt.isEmpty()) {
            player.sendMessage(Component.text("Unknown module: " + moduleId).color(TextColor.color(0xFF5555)));
            return false;
        }

        HideoutModule module = moduleOpt.get();
        int currentLevel = data.getModuleLevel(moduleId);
        int nextLevel = currentLevel + 1;

        if (nextLevel > module.maxTier()) {
            player.sendMessage(Component.text("✦ " + module.name() + " is already at max tier.")
                    .color(TextColor.color(0xAAAAAA)));
            return false;
        }

        Optional<ModuleTier> tierOpt = module.tier(nextLevel);
        if (tierOpt.isEmpty()) return false;

        ModuleTier tier = tierOpt.get();

        // Check materials
        for (var entry : tier.cost().entrySet()) {
            if (!hasEnough(player, entry.getKey(), entry.getValue())) {
                player.sendMessage(Component.text("✦ Need " + entry.getValue() + "x " + entry.getKey()
                        + " to upgrade " + module.name() + ".")
                        .color(TextColor.color(0xFF5555)));
                return false;
            }
        }

        // Consume materials
        for (var entry : tier.cost().entrySet()) {
            consumeItems(player, entry.getKey(), entry.getValue());
        }

        data.setModuleLevel(moduleId, nextLevel);
        player.sendMessage(Component.text("✦ " + module.name() + " upgraded to Tier " + nextLevel + "!")
                .color(TextColor.color(0x55FF55)));
        return true;
    }

    private String getOrCreateWorldName(UUID uuid) {
        return worldNames.computeIfAbsent(uuid, u -> "hideout_" + u.toString().substring(0, 8));
    }

    private void startIdleUnloadTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Iterator<Map.Entry<String, Long>> it = worldActivity.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                String worldName = entry.getKey();
                long lastActivity = entry.getValue();
                if (now - lastActivity > IDLE_UNLOAD_TICKS * 50L) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null && world.getPlayers().isEmpty()) {
                        plugin.getServer().unloadWorld(world, true);
                        it.remove();
                    }
                }
            }
        }, IDLE_UNLOAD_TICKS, IDLE_UNLOAD_TICKS);
    }

    private boolean hasEnough(Player player, String itemId, int required) {
        int count = 0;
        for (var stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            if (itemId.equals(ItemRegistry.getItemId(stack))) count += stack.getAmount();
        }
        return count >= required;
    }

    private void consumeItems(Player player, String itemId, int amount) {
        int remaining = amount;
        for (var stack : player.getInventory().getContents()) {
            if (stack == null || remaining <= 0) continue;
            if (!itemId.equals(ItemRegistry.getItemId(stack))) continue;
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
        }
    }
}
