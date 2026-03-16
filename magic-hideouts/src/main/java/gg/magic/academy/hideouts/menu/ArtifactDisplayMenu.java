package gg.magic.academy.hideouts.menu;

import gg.magic.academy.api.artifact.ArtifactTemplate;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.items.registry.ArtifactRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * GUI for the Artifact Display hideout module.
 * Players can activate / deactivate artifacts to gain their stat bonuses.
 * Slots available = Artifact Display module tier (max 2 active at once).
 */
public class ArtifactDisplayMenu {

    public static final String TITLE = "✦ Artifact Display";
    /** moduleId matching the YAML key for the artifact display module */
    public static final String MODULE_ID = "artifact_display";

    private final JavaPlugin plugin;
    private final ArtifactRegistry artifactRegistry;
    private final NamespacedKey artifactKey;

    public ArtifactDisplayMenu(JavaPlugin plugin, ArtifactRegistry artifactRegistry) {
        this.plugin = plugin;
        this.artifactRegistry = artifactRegistry;
        this.artifactKey = new NamespacedKey(plugin, "display_artifact_id");
    }

    public void open(Player player) {
        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        int moduleLevel = data.getModuleLevel(MODULE_ID);
        if (moduleLevel == 0) {
            player.sendMessage(Component.text("✦ You need to build the Artifact Display module first.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }
        int maxActive = moduleLevel; // T1 = 1 active, T2 = 2 active

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE));

        // Row 0 (slots 0-8): info bar
        for (int i = 0; i < 9; i++) inv.setItem(i, buildGlass(Material.PURPLE_STAINED_GLASS_PANE));

        inv.setItem(4, buildInfoItem(data, maxActive));

        // Rows 1-4 (slots 9-44): owned artifacts
        int slot = 9;
        for (ArtifactTemplate template : artifactRegistry.getAll()) {
            if (!data.hasArtifact(template.id())) continue;
            if (slot >= 45) break;
            boolean active = data.getActiveArtifacts().contains(template.id());
            inv.setItem(slot++, buildArtifactItem(template, active, data, maxActive));
        }

        // Row 5 (slots 45-53): bottom filler
        for (int i = 45; i < 54; i++) inv.setItem(i, buildGlass(Material.BLACK_STAINED_GLASS_PANE));

        player.openInventory(inv);
    }

    private ItemStack buildArtifactItem(ArtifactTemplate template, boolean active,
                                        MagicPlayerData data, int maxActive) {
        ItemStack base = artifactRegistry.buildItemStack(template.id());
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;

        List<Component> lore = new ArrayList<>(meta.lore() != null ? meta.lore() : List.of());

        lore.add(Component.empty());
        if (active) {
            lore.add(Component.text("● ACTIVE — Click to deactivate")
                    .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false));
        } else if (data.getActiveArtifacts().size() >= maxActive) {
            lore.add(Component.text("✗ No display slots free (T" + maxActive + " limit)")
                    .color(TextColor.color(0xFF5555)).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("○ Inactive — Click to activate")
                    .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        // Glow effect for active artifacts
        if (active) meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(artifactKey, PersistentDataType.STRING, template.id());
        base.setItemMeta(meta);
        return base;
    }

    private ItemStack buildInfoItem(MagicPlayerData data, int maxActive) {
        ItemStack stack = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Artifact Display")
                .color(TextColor.color(0xAA55FF)).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Active: " + data.getActiveArtifacts().size() + " / " + maxActive)
                        .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false),
                Component.text("Click artifacts to toggle them.")
                        .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false)
        ));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildGlass(Material mat) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }

    public NamespacedKey getArtifactKey() { return artifactKey; }
}
