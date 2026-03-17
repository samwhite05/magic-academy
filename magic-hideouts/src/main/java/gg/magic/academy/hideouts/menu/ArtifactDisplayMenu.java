package gg.magic.academy.hideouts.menu;

import gg.magic.academy.api.artifact.ArtifactTemplate;
import gg.magic.academy.api.gui.GuiUtil;
import gg.magic.academy.api.gui.PagedGui;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.items.registry.ArtifactRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArtifactDisplayMenu {

    public static final String TITLE = "✦ Artifact Display";
    public static final String MODULE_ID = "artifact_display";

    private final ArtifactRegistry artifactRegistry;

    public ArtifactDisplayMenu(ArtifactRegistry artifactRegistry) {
        this.artifactRegistry = artifactRegistry;
    }

    public void open(Player player) {
        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;

        int moduleLevel = data.getModuleLevel(MODULE_ID);
        if (moduleLevel == 0) {
            player.sendMessage(Component.text("✦ You need to build the Artifact Display module first.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }
        int maxActive = moduleLevel;

        PagedGui gui = new PagedGui(Component.text(TITLE));

        // Info item at slot 38 (row 5, col 3 in a 5-row GUI)
        gui.setFixed(38, GuiUtil.make(Material.ITEM_FRAME,
                Component.text("Artifact Display")
                        .color(TextColor.color(0xAA55FF)).decoration(TextDecoration.ITALIC, false),
                List.of(
                        Component.text("Active: " + data.getActiveArtifacts().size() + " / " + maxActive)
                                .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false),
                        Component.text("Click artifacts to toggle them.")
                                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false)
                )));

        for (ArtifactTemplate template : artifactRegistry.getAll()) {
            if (!data.hasArtifact(template.id())) continue;

            boolean active = data.getActiveArtifacts().contains(template.id());
            String artifactId = template.id();

            ItemStack base = artifactRegistry.buildItemStack(artifactId);
            ItemStack display = GuiUtil.make(base.getType(),
                    Component.text(template.name())
                            .color(template.rarity().color()).decoration(TextDecoration.ITALIC, false),
                    buildArtifactLore(template, active, data, maxActive));
            if (active) GuiUtil.glow(display);

            gui.addItem(display, e -> {
                if (e.getWhoClicked() instanceof Player p) handleArtifactClick(p, artifactId, active, maxActive);
            });
        }

        gui.open(player, 0);
    }

    private List<Component> buildArtifactLore(ArtifactTemplate template, boolean active,
                                               MagicPlayerData data, int maxActive) {
        List<Component> lore = new ArrayList<>();
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
        return lore;
    }

    private void handleArtifactClick(Player player, String artifactId, boolean currentlyActive, int maxActive) {
        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;

        if (currentlyActive) {
            data.deactivateArtifact(artifactId);
            player.sendMessage(Component.text("✦ " + artifactId + " deactivated.")
                    .color(TextColor.color(0xFFAA00)));
        } else {
            if (data.getActiveArtifacts().size() >= maxActive) {
                player.sendMessage(Component.text("✦ No display slots available! Upgrade your Artifact Display.")
                        .color(TextColor.color(0xFF5555)));
                return;
            }
            data.activateArtifact(artifactId);
            player.sendMessage(Component.text("✦ " + artifactId + " activated!")
                    .color(TextColor.color(0x55FF55)));
        }

        open(player);
    }
}
