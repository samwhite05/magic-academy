package gg.magic.academy.hideouts.menu;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ArtifactDisplayListener implements Listener {

    private final ArtifactDisplayMenu menu;

    public ArtifactDisplayListener(ArtifactDisplayMenu menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMenu(event)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (slot < 9 || slot >= 45) return; // only artifact area

        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String artifactId = meta.getPersistentDataContainer().get(menu.getArtifactKey(), PersistentDataType.STRING);
        if (artifactId == null) return;

        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        int moduleLevel = data.getModuleLevel(ArtifactDisplayMenu.MODULE_ID);
        int maxActive = moduleLevel;

        if (data.getActiveArtifacts().contains(artifactId)) {
            data.deactivateArtifact(artifactId);
            player.sendMessage(Component.text("Artifact deactivated.")
                    .color(TextColor.color(0xAAAAAA)));
        } else {
            if (data.getActiveArtifacts().size() >= maxActive) {
                player.sendMessage(Component.text("No display slots free. Deactivate another artifact first.")
                        .color(TextColor.color(0xFF5555)));
            } else {
                data.activateArtifact(artifactId);
                player.sendMessage(Component.text("Artifact activated!")
                        .color(TextColor.color(0x55FF55)));
            }
        }

        menu.open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isMenu(event)) return;
        event.setCancelled(true);
    }

    private boolean isMenu(InventoryClickEvent event) {
        return ArtifactDisplayMenu.TITLE.equals(
                PlainTextComponentSerializer.plainText().serialize(event.getView().title()));
    }

    private boolean isMenu(InventoryDragEvent event) {
        return ArtifactDisplayMenu.TITLE.equals(
                PlainTextComponentSerializer.plainText().serialize(event.getView().title()));
    }
}

