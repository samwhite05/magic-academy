package gg.magic.academy.hideouts.menu;

import gg.magic.academy.hideouts.manager.HideoutManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ModuleUpgradeListener implements Listener {

    private final ModuleUpgradeMenu menu;
    private final HideoutManager hideoutManager;

    public ModuleUpgradeListener(ModuleUpgradeMenu menu, HideoutManager hideoutManager) {
        this.menu = menu;
        this.hideoutManager = hideoutManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMenu(event)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String moduleId = meta.getPersistentDataContainer()
                .get(menu.getModuleIdKey(), PersistentDataType.STRING);
        if (moduleId == null || moduleId.isBlank()) return;

        hideoutManager.upgradeModule(player, moduleId);
        menu.open(player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!isMenu(event)) return;
        // no state to clear yet
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isMenu(event)) return;
        event.setCancelled(true);
    }

    private boolean isMenu(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        return ModuleUpgradeMenu.MENU_TITLE.equals(title);
    }

    private boolean isMenu(InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        return ModuleUpgradeMenu.MENU_TITLE.equals(title);
    }

    private boolean isMenu(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        return ModuleUpgradeMenu.MENU_TITLE.equals(title);
    }
}
