package gg.magic.academy.trial;

import gg.magic.academy.MagicAcademyPlugin;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.player.PlayerDataManager;
import gg.magic.academy.rank.RankManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages academy trials — solo boss fights that gate rank advancement.
 *
 * Trials use the academy's trial arena world. Each trial spawns a specific
 * MythicMobs boss; when it dies, the player advances rank.
 */
public class TrialManager {

    private final JavaPlugin plugin;
    private final RankManager rankManager;

    /** Players currently in a trial: UUID -> trial boss entity UUID */
    private final Map<UUID, UUID> activeTrials = new HashMap<>();

    public TrialManager(JavaPlugin plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    public void startTrial(Player player, String trialId) {
        if (activeTrials.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("✦ You are already in a trial!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        PlayerDataManager pdm = (PlayerDataManager) MagicAcademyPlugin.getCoreAPI().getPlayerDataManager();
        MagicPlayerData data = pdm.get(player);
        if (data == null) return;

        if (!rankManager.meetsPreTrialRequirements(player, data)) return;

        // Teleport to trial arena
        World trialWorld = Bukkit.getWorld("academy_trials");
        if (trialWorld == null) {
            trialWorld = Bukkit.createWorld(new WorldCreator("academy_trials")
                    .type(WorldType.FLAT)
                    .generateStructures(false));
        }
        if (trialWorld == null) {
            player.sendMessage(Component.text("✦ Trial arena not loaded. Contact staff.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Location spawnLoc = trialWorld.getSpawnLocation();
        player.teleport(spawnLoc);
        player.sendMessage(Component.text("✦ Trial begins! Defeat the guardian to advance your rank.")
                .color(TextColor.color(0xFFAA00)));

        // Spawn trial boss via MythicMobs
        spawnTrialBoss(player, trialId, spawnLoc.add(5, 0, 0));
    }

    private void spawnTrialBoss(Player player, String trialId, Location location) {
        try {
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("spawnMythicMob", String.class, Location.class)
                    .invoke(api, trialId, location);
            UUID bossUUID = extractEntityUuid(result);
            if (bossUUID != null) {
                activeTrials.put(player.getUniqueId(), bossUUID);
            } else {
                plugin.getLogger().warning("Failed to resolve trial boss UUID for " + trialId + ". Using fallback timer.");
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> completeTrial(player), 200L);
            }
        } catch (Exception e) {
            // MythicMobs not available — spawn a vanilla mob as placeholder
            var zombie = location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.ZOMBIE);
            activeTrials.put(player.getUniqueId(), zombie.getUniqueId());
            // Auto-complete trial after 10 seconds as placeholder
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> completeTrial(player), 200L);
        }
    }

    /** Called when the trial boss dies (hooked via MythicMobDeathEvent in the main plugin). */
    public void completeTrial(Player player) {
        if (!activeTrials.containsKey(player.getUniqueId())) return;
        activeTrials.remove(player.getUniqueId());

        PlayerDataManager pdm = (PlayerDataManager) MagicAcademyPlugin.getCoreAPI().getPlayerDataManager();
        MagicPlayerData data = pdm.get(player);
        if (data == null) return;

        player.sendMessage(Component.text("✦ Trial complete! You have proven yourself worthy.")
                .color(TextColor.color(0x55FF55)));

        rankManager.advanceRank(player, data);

        // Teleport back to academy
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            World academy = Bukkit.getWorld("world");
            if (academy != null) player.teleport(academy.getSpawnLocation());
        }, 60L);
    }

    public boolean isInTrial(UUID playerUuid) { return activeTrials.containsKey(playerUuid); }
    public Optional<UUID> getTrialBoss(UUID playerUuid) { return Optional.ofNullable(activeTrials.get(playerUuid)); }
    public void handleBossDeath(UUID bossUuid) {
        UUID playerUuid = null;
        for (var entry : activeTrials.entrySet()) {
            if (entry.getValue().equals(bossUuid)) {
                playerUuid = entry.getKey();
                break;
            }
        }
        if (playerUuid == null) return;
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            completeTrial(player);
        } else {
            activeTrials.remove(playerUuid);
        }
    }

    private UUID extractEntityUuid(Object mythicSpawnResult) {
        if (mythicSpawnResult == null) return null;
        if (mythicSpawnResult instanceof org.bukkit.entity.Entity entity) {
            return entity.getUniqueId();
        }
        try {
            Object entity = mythicSpawnResult.getClass().getMethod("getEntity").invoke(mythicSpawnResult);
            if (entity instanceof org.bukkit.entity.Entity bukkitEntity) {
                return bukkitEntity.getUniqueId();
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            return (UUID) mythicSpawnResult.getClass().getMethod("getUniqueId").invoke(mythicSpawnResult);
        } catch (Exception ignored) {
            return null;
        }
    }
}