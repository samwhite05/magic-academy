package gg.magic.academy.spells.discovery;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveriesListener implements Listener {

    private final DiscoveriesMenu menu;
    private final Map<UUID, Integer> currentPage = new ConcurrentHashMap<>();

    public DiscoveriesListener(DiscoveriesMenu menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMenu(event)) return;

        event.setCancelled(true);

        // Only process top-inventory clicks
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Check for page navigation arrow
        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        Integer targetPage = meta.getPersistentDataContainer().get(menu.pageKey, PersistentDataType.INTEGER);
        if (targetPage != null) {
            currentPage.put(player.getUniqueId(), targetPage);
            menu.open(player, targetPage);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isMenu(event)) return;
        event.setCancelled(true);
    }

    private boolean isMenu(InventoryClickEvent event) {
        return DiscoveriesMenu.TITLE.equals(
                PlainTextComponentSerializer.plainText().serialize(event.getView().title()));
    }

    private boolean isMenu(InventoryDragEvent event) {
        return DiscoveriesMenu.TITLE.equals(
                PlainTextComponentSerializer.plainText().serialize(event.getView().title()));
    }
}
