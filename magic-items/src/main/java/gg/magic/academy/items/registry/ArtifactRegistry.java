package gg.magic.academy.items.registry;

import gg.magic.academy.api.Rarity;
import gg.magic.academy.api.artifact.ArtifactEffectHandler;
import gg.magic.academy.api.artifact.ArtifactSource;
import gg.magic.academy.api.artifact.ArtifactTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.core.artifact.ArtifactEffectRegistry;
import gg.magic.academy.core.artifact.ArtifactStatProvider;
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
 * Loads ArtifactTemplate definitions from plugins/MagicAcademy/artifacts/*.yml,
 * builds ItemStack representations, and registers each artifact's stat effect
 * with MagicCore's ArtifactStatProvider.
 */
public class ArtifactRegistry {

    public static final String PDC_KEY = "artifact_id";

    private final JavaPlugin plugin;
    private final NamespacedKey artifactKey;
    private final Map<String, ArtifactTemplate> templates = new LinkedHashMap<>();

    public ArtifactRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.artifactKey = new NamespacedKey(plugin, PDC_KEY);
    }

    public void loadAll() {
        templates.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(),
                "plugins/MagicAcademy/artifacts");
        if (!dir.exists()) { dir.mkdirs(); return; }

        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        ArtifactEffectRegistry effectReg = MagicCore.get().getArtifactEffectRegistry();
        ArtifactStatProvider statProvider = MagicCore.get().getArtifactStatProvider();

        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String id : cfg.getKeys(false)) {
                try {
                    ConfigurationSection s = cfg.getConfigurationSection(id);
                    if (s == null) continue;

                    String name      = s.getString("name", id);
                    String sourceStr = s.getString("source", "DUNGEON_SECRET_ROOM");
                    String effectId  = s.getString("effectId", "");
                    String rarityStr = s.getString("rarity", "COMMON");
                    int cmd          = s.getInt("customModelData", 0);

                    ArtifactSource source;
                    try { source = ArtifactSource.valueOf(sourceStr); }
                    catch (IllegalArgumentException e) { source = ArtifactSource.DUNGEON_SECRET_ROOM; }

                    Rarity rarity;
                    try { rarity = Rarity.valueOf(rarityStr); }
                    catch (IllegalArgumentException e) { rarity = Rarity.COMMON; }

                    ArtifactTemplate template = new ArtifactTemplate(id, name, source, effectId, rarity, cmd);
                    templates.put(id, template);

                    // Link this artifact to its effect handler
                    effectReg.get(effectId).ifPresent(handler ->
                            statProvider.mapArtifact(id, handler));

                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load artifact: " + id, e);
                }
            }
        }
        plugin.getLogger().info("ArtifactRegistry: loaded " + templates.size() + " artifacts.");
    }

    /** Build a displayable ItemStack for an artifact. */
    public ItemStack buildItemStack(String artifactId) {
        ArtifactTemplate template = templates.get(artifactId);
        if (template == null) return new ItemStack(Material.BARRIER);

        ItemStack stack = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Component.text(template.name())
                .color(template.rarity().color())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(template.rarity().name())
                .color(template.rarity().color()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Source: " + template.source().name().replace('_', ' ').toLowerCase())
                .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));

        ArtifactEffectHandler h = MagicCore.get().getArtifactEffectRegistry()
                .get(template.effectId()).orElse(null);
        if (h != null) {
            lore.add(Component.empty());
            if (h.getManaBonus() != 0)
                lore.add(Component.text("Max Mana +" + h.getManaBonus())
                        .color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            if (h.getDamageBonus() != 0)
                lore.add(Component.text("Spell Damage +" + (int)(h.getDamageBonus()*100) + "%")
                        .color(TextColor.color(0xFFAA55)).decoration(TextDecoration.ITALIC, false));
            if (h.getManaRegenBonus() != 0)
                lore.add(Component.text("Mana Regen +" + h.getManaRegenBonus())
                        .color(TextColor.color(0x66FFCC)).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Display in your Hideout to activate")
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));

        if (template.customModelData() != 0) meta.setCustomModelData(template.customModelData());
        meta.getPersistentDataContainer().set(artifactKey, PersistentDataType.STRING, artifactId);
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public Optional<ArtifactTemplate> get(String id) { return Optional.ofNullable(templates.get(id)); }
    public Collection<ArtifactTemplate> getAll() { return Collections.unmodifiableCollection(templates.values()); }
    public int size() { return templates.size(); }

    /** Returns the artifact ID stored in the item's PDC, or null. */
    public static String getArtifactId(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey("magicitems", PDC_KEY);
        String val = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return val;
    }

    public NamespacedKey getArtifactKey() { return artifactKey; }
}
