package gg.magic.academy.hideouts.module;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads hideout module definitions from plugins/MagicAcademy/hideouts/modules.yml
 *
 * YAML format:
 *   spell_laboratory:
 *     name: "Spell Laboratory"
 *     description: "Enhances spell power and mana capacity."
 *     tiers:
 *       1:
 *         description: "Basic spell focus."
 *         cost: {mana_crystal: 10}
 *         max_mana_bonus: 20
 *         spell_damage_bonus: 0.05
 *         mana_regen_bonus: 1
 *       2:
 *         description: "Improved resonance chamber."
 *         cost: {void_mushroom: 8, mana_crystal: 20}
 *         max_mana_bonus: 40
 *         spell_damage_bonus: 0.10
 *         mana_regen_bonus: 2
 */
public class ModuleRegistry {

    private final JavaPlugin plugin;
    private final Map<String, HideoutModule> modules = new HashMap<>();

    public ModuleRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        modules.clear();
        File file = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/hideouts/modules.yml");
        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String moduleId : cfg.getKeys(false)) {
            try {
                ConfigurationSection ms = cfg.getConfigurationSection(moduleId);
                if (ms == null) continue;
                String name = ms.getString("name", moduleId);
                String desc = ms.getString("description", "");

                List<ModuleTier> tiers = new ArrayList<>();
                ConfigurationSection tiersSection = ms.getConfigurationSection("tiers");
                if (tiersSection != null) {
                    for (String tierKey : tiersSection.getKeys(false)) {
                        ConfigurationSection ts = tiersSection.getConfigurationSection(tierKey);
                        if (ts == null) continue;
                        int tier = Integer.parseInt(tierKey);
                        Map<String, Integer> cost = new HashMap<>();
                        ConfigurationSection costSec = ts.getConfigurationSection("cost");
                        if (costSec != null) {
                            for (String item : costSec.getKeys(false)) cost.put(item, costSec.getInt(item));
                        }
                        tiers.add(new ModuleTier(
                                tier,
                                ts.getString("description", ""),
                                cost,
                                ts.getInt("max_mana_bonus", 0),
                                ts.getDouble("spell_damage_bonus", 0.0),
                                ts.getInt("mana_regen_bonus", 0)
                        ));
                    }
                    tiers.sort(Comparator.comparingInt(ModuleTier::tier));
                }
                modules.put(moduleId, new HideoutModule(moduleId, name, desc, tiers));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load module: " + moduleId, e);
            }
        }
    }

    public Optional<HideoutModule> get(String id) { return Optional.ofNullable(modules.get(id)); }
    public Collection<HideoutModule> getAll() { return Collections.unmodifiableCollection(modules.values()); }
    public int size() { return modules.size(); }
}
