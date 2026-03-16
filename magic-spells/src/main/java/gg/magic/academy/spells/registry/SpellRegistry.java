package gg.magic.academy.spells.registry;

import gg.magic.academy.api.element.Element;
import gg.magic.academy.api.spell.SpellEffect;
import gg.magic.academy.api.spell.SpellShape;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.api.spell.SpellTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads all spell definitions from plugins/MagicAcademy/spells/*.yml
 */
public class SpellRegistry {

    private final JavaPlugin plugin;
    private final Map<String, SpellTemplate> spells = new HashMap<>();

    public SpellRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        spells.clear();
        File dir = getContentDir("spells");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            try {
                SpellTemplate spell = parse(cfg);
                spells.put(spell.id(), spell);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load spell from " + file.getName(), e);
            }
        }
    }

    private SpellTemplate parse(YamlConfiguration cfg) {
        String id = cfg.getString("id");
        String name = cfg.getString("name", id);
        Element element = Element.valueOf(cfg.getString("element", "ARCANE").toUpperCase());
        SpellShape shape = SpellShape.valueOf(cfg.getString("shape", "PROJECTILE").toUpperCase());
        SpellEffect effect = SpellEffect.valueOf(cfg.getString("effect", "NONE").toUpperCase());

        List<SpellTier> tiers = new ArrayList<>();
        ConfigurationSection tiersSection = cfg.getConfigurationSection("tiers");
        if (tiersSection != null) {
            for (String tierKey : tiersSection.getKeys(false)) {
                int tierNum = Integer.parseInt(tierKey);
                ConfigurationSection ts = tiersSection.getConfigurationSection(tierKey);
                String desc = ts.getString("description", "");
                int mana = ts.getInt("mana_cost", 10);
                long cooldown = ts.getLong("cooldown_ms", 1500);
                String mythicSkill = ts.getString("mythicmobs_skill", "");

                // Upgrade costs (from "upgrade_costs" section at spell root, keyed by tier)
                ConfigurationSection costRoot = cfg.getConfigurationSection("upgrade_costs");
                Map<String, Integer> upgradeCost = new HashMap<>();
                if (costRoot != null && costRoot.contains(tierKey)) {
                    ConfigurationSection costSection = costRoot.getConfigurationSection(tierKey);
                    if (costSection != null) {
                        for (String itemId : costSection.getKeys(false)) {
                            upgradeCost.put(itemId, costSection.getInt(itemId));
                        }
                    }
                }
                tiers.add(new SpellTier(tierNum, desc, mana, cooldown, mythicSkill, upgradeCost));
            }
            tiers.sort(Comparator.comparingInt(SpellTier::tier));
        }

        return new SpellTemplate(id, name, element, shape, effect, tiers);
    }

    public Optional<SpellTemplate> get(String id) { return Optional.ofNullable(spells.get(id)); }
    public Collection<SpellTemplate> getAll() { return Collections.unmodifiableCollection(spells.values()); }
    public int size() { return spells.size(); }

    private File getContentDir(String subdir) {
        return new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/" + subdir);
    }
}
