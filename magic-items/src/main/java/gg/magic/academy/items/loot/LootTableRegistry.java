package gg.magic.academy.items.loot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads loot table definitions from plugins/MagicAcademy/loot_tables/*.yml
 *
 * YAML format:
 *   ruins_treasure:
 *     rolls: 3
 *     guaranteed_chance: 0.85
 *     entries:
 *       - item: mana_crystal
 *         min: 1
 *         max: 3
 *         weight: 40
 *       - item: fire_rune
 *         min: 1
 *         max: 1
 *         weight: 15
 */
public class LootTableRegistry {

    private final JavaPlugin plugin;
    private final Map<String, LootTable> tables = new HashMap<>();

    public LootTableRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        tables.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/loot_tables");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String tableId : cfg.getKeys(false)) {
                ConfigurationSection section = cfg.getConfigurationSection(tableId);
                if (section == null) continue;
                try {
                    tables.put(tableId, parse(tableId, section));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load loot table: " + tableId, e);
                }
            }
        }
    }

    private LootTable parse(String id, ConfigurationSection s) {
        int rolls = s.getInt("rolls", 1);
        double chance = s.getDouble("guaranteed_chance", 1.0);
        List<LootEntry> entries = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("entries")) {
            String itemId = (String) raw.get("item");
            int min = raw.containsKey("min") ? (int) raw.get("min") : 1;
            int max = raw.containsKey("max") ? (int) raw.get("max") : 1;
            double weight = raw.containsKey("weight") ? ((Number) raw.get("weight")).doubleValue() : 10.0;
            entries.add(new LootEntry(itemId, min, max, weight));
        }
        return new LootTable(id, entries, rolls, chance);
    }

    public Optional<LootTable> get(String id) {
        return Optional.ofNullable(tables.get(id));
    }
}
