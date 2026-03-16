package gg.magic.academy.world.boss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Periodically announces and spawns world boss encounters.
 *
 * Config: plugins/MagicAcademy/world_bosses.yml
 *
 * YAML format:
 *   arcane_titan:
 *     name: "Arcane Titan"
 *     mythicmobs_mob: ArcaneTitan
 *     world: world
 *     spawn_location: {x: 0, y: 64, z: 0}
 *     interval_hours: 6
 *     announce_minutes_before: 5
 */
public class WorldBossScheduler {

    private final JavaPlugin plugin;
    private final List<WorldBossConfig> bossConfigs = new ArrayList<>();

    public WorldBossScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        File file = new File(plugin.getDataFolder().getParentFile().getParentFile(),
                "plugins/MagicAcademy/world_bosses.yml");
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            try {
                ConfigurationSection s = cfg.getConfigurationSection(id);
                ConfigurationSection loc = s.getConfigurationSection("spawn_location");
                bossConfigs.add(new WorldBossConfig(
                        id,
                        s.getString("name", id),
                        s.getString("mythicmobs_mob", id),
                        s.getString("world", "world"),
                        loc != null ? loc.getDouble("x") : 0,
                        loc != null ? loc.getDouble("y") : 64,
                        loc != null ? loc.getDouble("z") : 0,
                        s.getInt("interval_hours", 6),
                        s.getInt("announce_minutes_before", 5)
                ));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load world boss: " + id, e);
            }
        }
    }

    public void schedule() {
        for (WorldBossConfig boss : bossConfigs) {
            scheduleNext(boss);
        }
    }

    private void scheduleNext(WorldBossConfig boss) {
        long intervalTicks = boss.intervalHours() * 72000L;
        long announceTicks = boss.announceMinutesBefore() * 1200L;

        // Announce
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getServer().broadcast(
                    Component.text("⚠ " + boss.name() + " will appear in " + boss.announceMinutesBefore()
                            + " minutes! Prepare yourselves!")
                            .color(TextColor.color(0xFF0000))
            );
        }, intervalTicks - announceTicks);

        // Spawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            spawnBoss(boss);
            scheduleNext(boss); // reschedule for next interval
        }, intervalTicks);
    }

    private void spawnBoss(WorldBossConfig boss) {
        World world = plugin.getServer().getWorld(boss.world());
        if (world == null) {
            plugin.getLogger().warning("World boss world not found: " + boss.world());
            return;
        }
        Location loc = new Location(world, boss.x(), boss.y(), boss.z());

        plugin.getServer().broadcast(
                Component.text("⚔ " + boss.name() + " has appeared! All mages to battle!")
                        .color(TextColor.color(0xFF4400))
        );

        try {
            Class<?> apiClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            apiClass.getMethod("spawnMythicMob", String.class, Location.class)
                    .invoke(api, boss.mythicmobsMob(), loc);
        } catch (Exception e) {
            world.spawnEntity(loc, org.bukkit.entity.EntityType.ELDER_GUARDIAN);
        }
    }

    private record WorldBossConfig(
            String id, String name, String mythicmobsMob,
            String world, double x, double y, double z,
            int intervalHours, int announceMinutesBefore
    ) {}
}
