package gg.magic.academy.world.zone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Defines rectangular world zones (no WorldGuard dependency).
 * Loads zone definitions from plugins/MagicAcademy/zones/*.yml
 *
 * YAML format:
 *   forbidden_forest:
 *     name: "Forbidden Forest"
 *     world: world
 *     min: {x: -500, y: 0, z: 200}
 *     max: {x: -200, y: 256, z: 500}
 *     enter_message: "§4⚠ You enter the Forbidden Forest. Danger lurks here."
 *     mob_table: forbidden_forest_mobs
 *     ambient_effect: DARKNESS
 */
public class ZoneManager implements Listener {

    private final JavaPlugin plugin;
    private final List<WorldZone> zones = new ArrayList<>();

    /** player UUID -> current zone id */
    private final Map<UUID, String> playerZone = new HashMap<>();

    public ZoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadZones();
    }

    private void loadZones() {
        zones.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/zones");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String zoneId : cfg.getKeys(false)) {
                try {
                    ConfigurationSection s = cfg.getConfigurationSection(zoneId);
                    if (s == null) continue;
                    ConfigurationSection min = s.getConfigurationSection("min");
                    ConfigurationSection max = s.getConfigurationSection("max");
                    zones.add(new WorldZone(
                            zoneId,
                            s.getString("name", zoneId),
                            s.getString("world", "world"),
                            min.getDouble("x"), min.getDouble("y"), min.getDouble("z"),
                            max.getDouble("x"), max.getDouble("y"), max.getDouble("z"),
                            s.getString("enter_message", ""),
                            s.getString("mob_table", ""),
                            s.getString("ambient_effect", "")
                    ));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load zone: " + zoneId, e);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        Location loc = event.getTo();

        String currentZoneId = playerZone.get(player.getUniqueId());
        WorldZone newZone = getZoneAt(loc);
        String newZoneId = newZone != null ? newZone.id() : null;

        if (!Objects.equals(currentZoneId, newZoneId)) {
            if (newZone != null && !newZone.enterMessage().isEmpty()) {
                player.sendMessage(Component.text(newZone.enterMessage())
                        .color(TextColor.color(0xFF8800)));
            }
            if (newZoneId != null) {
                playerZone.put(player.getUniqueId(), newZoneId);
            } else {
                playerZone.remove(player.getUniqueId());
            }
        }
    }

    public WorldZone getZoneAt(Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "";
        for (WorldZone zone : zones) {
            if (!zone.world().equals(world)) continue;
            if (loc.getX() >= zone.minX() && loc.getX() <= zone.maxX()
                    && loc.getY() >= zone.minY() && loc.getY() <= zone.maxY()
                    && loc.getZ() >= zone.minZ() && loc.getZ() <= zone.maxZ()) {
                return zone;
            }
        }
        return null;
    }

    public Optional<String> getPlayerZone(UUID uuid) { return Optional.ofNullable(playerZone.get(uuid)); }
}
