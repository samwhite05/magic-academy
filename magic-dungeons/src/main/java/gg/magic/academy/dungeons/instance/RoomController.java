package gg.magic.academy.dungeons.instance;

import gg.magic.academy.dungeons.template.RoomConfig;
import gg.magic.academy.items.MagicItems;
import gg.magic.academy.items.loot.LootTable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;
import java.util.UUID;

/**
 * Controls the logic for a single dungeon room.
 * Calls the onComplete Runnable when the room is finished.
 */
public class RoomController {

    private final JavaPlugin plugin;
    private final DungeonInstance instance;
    private final RoomConfig room;
    private final DungeonInstanceManager instanceManager;
    private final Runnable onComplete;
    private final Random rng = new Random();

    public RoomController(JavaPlugin plugin, DungeonInstance instance, RoomConfig room, DungeonInstanceManager instanceManager, Runnable onComplete) {
        this.plugin = plugin;
        this.instance = instance;
        this.room = room;
        this.instanceManager = instanceManager;
        this.onComplete = onComplete;
    }

    public void start(Player player) {
        switch (room.type()) {
            case PUZZLE -> startPuzzle(player);
            case MOB_WAVE -> startMobWave(player);
            case MINIBOSS -> spawnBoss(player, room.mythicmobsMob(), "Miniboss");
            case BOSS -> spawnBoss(player, room.mythicmobsMob(), "Boss");
            case TREASURE -> openTreasure(player);
        }
    }

    private void startPuzzle(Player player) {
        player.sendMessage(Component.text("✦ Puzzle Room — Solve the challenge to proceed.")
                .color(TextColor.color(0x00AAFF)));
        // For now, auto-complete after delay (puzzle logic is map-builder territory)
        // Real puzzle triggers come from pressure plates / block events handled by magic-world
        plugin.getServer().getScheduler().runTaskLater(plugin, onComplete, 100L);
    }

    private void startMobWave(Player player) {
        player.sendMessage(Component.text("✦ Wave Room — Survive " + room.waveCount() + " waves!")
                .color(TextColor.color(0xFF5555)));
        spawnWave(player, room.waveCount(), 0);
    }

    private void spawnWave(Player player, int totalWaves, int current) {
        if (current >= totalWaves) {
            player.sendMessage(Component.text("✦ All waves defeated!")
                    .color(TextColor.color(0x55FF55)));
            plugin.getServer().getScheduler().runTaskLater(plugin, onComplete, 40L);
            return;
        }
        player.sendMessage(Component.text("✦ Wave " + (current + 1) + "/" + totalWaves)
                .color(TextColor.color(0xFFAA00)));
        spawnMythicMobs(player.getWorld(), room.mythicmobsPack(), player.getLocation());

        // Advance to next wave after a delay (real impl: advance when all mobs die)
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                spawnWave(player, totalWaves, current + 1), 200L);
    }

    private void spawnBoss(Player player, String mobId, String label) {
        player.sendMessage(Component.text("✦ " + label + " — " + mobId + " has appeared!")
                .color(TextColor.color(0xFF0000)));
        UUID bossUuid = spawnBossEntity(player.getWorld(), mobId, player.getLocation());
        if (bossUuid != null) {
            instance.setActiveBossUuid(bossUuid);
            instanceManager.registerBoss(instance.getInstanceId(), bossUuid, onComplete);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, onComplete, 400L);
        }
    }

    private void openTreasure(Player player) {
        player.sendMessage(Component.text("✦ Treasure Room — Claim your reward!")
                .color(TextColor.color(0xFFFF55)));

        if (!room.lootTableId().isEmpty() && MagicItems.get() != null) {
            MagicItems.get().getLootTableRegistry().get(room.lootTableId()).ifPresent(lootTable -> {
                var items = lootTable.roll(MagicItems.get().getItemRegistry(), rng);
                for (var item : items) {
                    player.getInventory().addItem(item);
                }
                player.sendMessage(Component.text("✦ You received " + items.size() + " item(s).")
                        .color(TextColor.color(0x55FF55)));
            });
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, onComplete, 60L);
    }

    private void spawnMythicMobs(World world, String mobIdOrPack, Location location) {
        try {
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            apiClass.getMethod("spawnMythicMob", String.class, Location.class)
                    .invoke(api, mobIdOrPack, location);
        } catch (Exception e) {
            // MythicMobs not installed — spawn a vanilla zombie as placeholder
            world.spawnEntity(location, org.bukkit.entity.EntityType.ZOMBIE);
        }
    }

    private UUID spawnBossEntity(World world, String mobId, Location location) {
        try {
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("spawnMythicMob", String.class, Location.class)
                    .invoke(api, mobId, location);
            UUID uuid = extractEntityUuid(result);
            if (uuid != null) return uuid;
        } catch (Exception e) {
            // fall through to vanilla
        }
        return world.spawnEntity(location, org.bukkit.entity.EntityType.ZOMBIE).getUniqueId();
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