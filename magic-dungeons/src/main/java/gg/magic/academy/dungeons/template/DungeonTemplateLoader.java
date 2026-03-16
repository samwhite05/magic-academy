package gg.magic.academy.dungeons.template;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads dungeon definitions from plugins/MagicAcademy/dungeons/*.yml
 */
public class DungeonTemplateLoader {

    private final JavaPlugin plugin;
    private final Map<String, DungeonTemplate> templates = new HashMap<>();

    public DungeonTemplateLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        templates.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/dungeons");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                DungeonTemplate template = parse(cfg);
                templates.put(template.id(), template);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load dungeon: " + file.getName(), e);
            }
        }
    }

    private DungeonTemplate parse(YamlConfiguration cfg) {
        String id = cfg.getString("id");
        String name = cfg.getString("name", id);
        String theme = cfg.getString("theme", "RUINS");
        int difficulty = cfg.getInt("difficulty_base", 1);
        String requiredRank = cfg.getString("required_rank", "STUDENT");

        ConfigurationSection scaleSec = cfg.getConfigurationSection("scale");
        double soloHp = scaleSec != null ? scaleSec.getDouble("solo_hp_mult", 0.6) : 0.6;
        double partyHp = scaleSec != null ? scaleSec.getDouble("party_hp_mult", 1.0) : 1.0;
        double perExtra = scaleSec != null ? scaleSec.getDouble("per_extra_player_add", 0.3) : 0.3;

        List<RoomConfig> rooms = new ArrayList<>();
        for (Map<?, ?> raw : cfg.getMapList("rooms")) {
            RoomConfig.RoomType type = RoomConfig.RoomType.valueOf(((String) raw.get("type")).toUpperCase());
            String config = getString(raw, "config", "");
            String mobPack = getString(raw, "mythicmobs_pack", "");
            int waves = getInt(raw, "wave_count", 1);
            String mob = getString(raw, "mythicmobs_mob", "");
            String loot = getString(raw, "loot_table", "");
            rooms.add(new RoomConfig(type, config, mobPack, waves, mob, loot));
        }

        return new DungeonTemplate(id, name, theme, difficulty, rooms, soloHp, partyHp, perExtra, requiredRank);
    }

    public Optional<DungeonTemplate> get(String id) { return Optional.ofNullable(templates.get(id)); }
    public Collection<DungeonTemplate> getAll() { return Collections.unmodifiableCollection(templates.values()); }
    public int size() { return templates.size(); }

    private String getString(Map<?, ?> raw, String key, String def) {
        Object value = raw.get(key);
        if (value == null) return def;
        return String.valueOf(value);
    }

    private int getInt(Map<?, ?> raw, String key, int def) {
        Object value = raw.get(key);
        if (value == null) return def;
        if (value instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
