package gg.magic.academy.contract;

import gg.magic.academy.api.Rarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads ContractTemplate definitions from plugins/MagicAcademy/contracts/*.yml
 */
public class ContractRegistry {

    private final JavaPlugin plugin;
    private final Map<String, ContractTemplate> contracts = new LinkedHashMap<>();

    public ContractRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        contracts.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(),
                "plugins/MagicAcademy/contracts");
        if (!dir.exists()) { dir.mkdirs(); return; }

        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String id : cfg.getKeys(false)) {
                try {
                    ConfigurationSection s = cfg.getConfigurationSection(id);
                    if (s == null) continue;

                    String name    = s.getString("name", id);
                    String desc    = s.getString("description", "");
                    Rarity rarity;
                    try { rarity = Rarity.valueOf(s.getString("rarity", "COMMON")); }
                    catch (IllegalArgumentException e) { rarity = Rarity.COMMON; }

                    List<ContractObjective> objectives = new ArrayList<>();
                    for (Map<?, ?> rawMap : s.getMapList("objectives")) {
                        @SuppressWarnings("unchecked")
                        Map<Object, Object> objMap = (Map<Object, Object>) rawMap;
                        String typeStr  = String.valueOf(objMap.getOrDefault("type", "KILL_MOB"));
                        String target   = String.valueOf(objMap.getOrDefault("target", "ZOMBIE"));
                        int required    = ((Number) objMap.getOrDefault("required", 1)).intValue();
                        String label    = String.valueOf(objMap.getOrDefault("label", target));
                        ContractObjective.Type type;
                        try { type = ContractObjective.Type.valueOf(typeStr.toUpperCase()); }
                        catch (IllegalArgumentException e) { type = ContractObjective.Type.KILL_MOB; }
                        objectives.add(new ContractObjective(type, target, required, label));
                    }

                    Map<String, Integer> rewardItems = new LinkedHashMap<>();
                    ConfigurationSection rewards = s.getConfigurationSection("rewards.items");
                    if (rewards != null) {
                        for (String itemId : rewards.getKeys(false)) {
                            rewardItems.put(itemId, rewards.getInt(itemId, 1));
                        }
                    }
                    int rewardMana = s.getInt("rewards.mana", 0);

                    contracts.put(id, new ContractTemplate(id, name, desc, rarity, objectives, rewardItems, rewardMana));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load contract: " + id, e);
                }
            }
        }
        plugin.getLogger().info("ContractRegistry: loaded " + contracts.size() + " contracts.");
    }

    public Optional<ContractTemplate> get(String id) { return Optional.ofNullable(contracts.get(id)); }
    public Collection<ContractTemplate> getAll() { return Collections.unmodifiableCollection(contracts.values()); }
    public int size() { return contracts.size(); }
}
