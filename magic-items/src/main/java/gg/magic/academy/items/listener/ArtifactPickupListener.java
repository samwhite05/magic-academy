package gg.magic.academy.items.listener;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Grants artifact ownership in MagicPlayerData when a player picks up an artifact item.
 */
public class ArtifactPickupListener implements Listener {

    private final NamespacedKey artifactKey;

    public ArtifactPickupListener(JavaPlugin plugin) {
        // Must match ArtifactRegistry's key: namespace = plugin name lowercase, key = "artifact_id"
        this.artifactKey = new NamespacedKey(plugin, "artifact_id");
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemMeta meta = event.getItem().getItemStack().getItemMeta();
        if (meta == null) return;

        String artifactId = meta.getPersistentDataContainer().get(artifactKey, PersistentDataType.STRING);
        if (artifactId == null || artifactId.isBlank()) return;

        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        if (!data.hasArtifact(artifactId)) {
            data.grantArtifact(artifactId);
            player.sendMessage(net.kyori.adventure.text.Component.text(
                            "Artifact obtained: " + artifactId.replace('_', ' '))
                    .color(net.kyori.adventure.text.format.TextColor.color(0xFFAA00)));
        }
    }
}

