package gg.magic.academy.items.registry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads all custom item definitions from plugins/MagicAcademy/items/*.yml
 * and builds cached ItemStack templates.
 *
 * YAML format per item:
 *   id: void_mushroom
 *   name: "Void Mushroom"
 *   material: CRIMSON_FUNGUS
 *   custom_model_data: 2001
 *   color: "#AA00AA"          # optional hex color for name
 *   lore:
 *     - "A mushroom that grows in the void."
 *   tags:
 *     - ingredient
 */
public class ItemRegistry {

    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("magic", "item_id");

    private final JavaPlugin plugin;
    private final Map<String, ItemStack> templates = new HashMap<>();

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        templates.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/items");
        if (!dir.exists()) {
            dir.mkdirs();
            plugin.saveResource("items/defaults.yml", false);
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                ConfigurationSection section = cfg.getConfigurationSection(key);
                if (section == null) continue;
                try {
                    register(section);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load item: " + key + " in " + file.getName(), e);
                }
            }
        }
    }

    private void register(ConfigurationSection s) {
        String id = s.getString("id");
        String name = s.getString("name", id);
        Material material = Material.matchMaterial(s.getString("material", "PAPER"));
        if (material == null) material = Material.PAPER;
        int cmd = s.getInt("custom_model_data", 0);
        String colorHex = s.getString("color", "#FFFFFF");
        List<String> loreStrings = s.getStringList("lore");

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        // Name
        TextColor color = TextColor.fromHexString(colorHex);
        meta.displayName(Component.text(name)
                .color(color)
                .decoration(TextDecoration.ITALIC, false));

        // Lore
        if (!loreStrings.isEmpty()) {
            List<Component> lore = loreStrings.stream()
                    .map(line -> (Component) Component.text(line)
                            .color(TextColor.color(0xAAAAAA))
                            .decoration(TextDecoration.ITALIC, false))
                    .toList();
            meta.lore(lore);
        }

        // Custom model data
        if (cmd > 0) meta.setCustomModelData(cmd);

        // Store item ID in PDC for identification
        meta.getPersistentDataContainer().set(ITEM_ID_KEY, PersistentDataType.STRING, id);

        stack.setItemMeta(meta);
        templates.put(id, stack);
    }

    /** Returns a copy of the template ItemStack for the given item ID. */
    public Optional<ItemStack> get(String id) {
        ItemStack template = templates.get(id);
        return template == null ? Optional.empty() : Optional.of(template.clone());
    }

    /** Returns a copy with the given quantity. */
    public Optional<ItemStack> get(String id, int amount) {
        return get(id).map(stack -> { stack.setAmount(amount); return stack; });
    }

    /** Extracts the magic item ID from a stack's PDC. Returns null if not a magic item. */
    public static String getItemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(ITEM_ID_KEY, PersistentDataType.STRING);
    }

    public int size() { return templates.size(); }

    public Set<String> getIds() { return Collections.unmodifiableSet(templates.keySet()); }
}
