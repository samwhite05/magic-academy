package gg.magic.academy.spells.registry;

import gg.magic.academy.api.rune.RuneTemplate;
import gg.magic.academy.api.rune.RuneType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads rune definitions from plugins/MagicAcademy/runes/*.yml
 * Also resolves the rune combination key (elementId:shapeId:effectId) -> spellId
 */
public class RuneRegistry {

    private final JavaPlugin plugin;
    private final Map<String, RuneTemplate> runes = new HashMap<>();

    /** element:shape:effect -> spellId */
    private final Map<String, String> recipeMap = new HashMap<>();

    public RuneRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        runes.clear();
        recipeMap.clear();

        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/runes");
        if (!dir.exists()) { dir.mkdirs(); return; }

        // Load individual rune definitions
        File[] runeFiles = dir.listFiles((d, name) -> name.endsWith(".yml") && !name.equals("recipes.yml"));
        if (runeFiles != null) {
            for (File file : runeFiles) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                try {
                    RuneTemplate rune = parseRune(cfg);
                    runes.put(rune.id(), rune);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load rune from " + file.getName(), e);
                }
            }
        }

        // Load recipe mapping
        File recipesFile = new File(dir, "recipes.yml");
        if (recipesFile.exists()) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(recipesFile);
            for (String key : cfg.getKeys(false)) {
                recipeMap.put(key, cfg.getString(key));
            }
        }
    }

    private RuneTemplate parseRune(YamlConfiguration cfg) {
        String id = cfg.getString("id");
        String name = cfg.getString("name", id);
        RuneType type = RuneType.valueOf(cfg.getString("type", "ELEMENT").toUpperCase());
        int tier = cfg.getInt("power_tier", 1);
        int cmd = cfg.getInt("custom_model_data", 0);
        return new RuneTemplate(id, name, type, tier, cmd);
    }

    /** Returns the spell ID for a given rune combo, or empty if unknown. */
    public Optional<String> resolveRecipe(String elementId, String shapeId, String effectId) {
        String key = elementId + ":" + shapeId + ":" + effectId;
        return Optional.ofNullable(recipeMap.get(key));
    }

    public Optional<RuneTemplate> get(String id) { return Optional.ofNullable(runes.get(id)); }
    public Collection<RuneTemplate> getAll() { return Collections.unmodifiableCollection(runes.values()); }
}
