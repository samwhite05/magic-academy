package gg.magic.academy.hideouts.hotbar;

import gg.magic.academy.hideouts.manager.HideoutManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class HideoutHotbarListener implements Listener {

    private final HideoutManager hideoutManager;
    private final NamespacedKey actionKey;

    public HideoutHotbarListener(HideoutManager hideoutManager) {
        this.hideoutManager = hideoutManager;
        this.actionKey = new NamespacedKey(
                gg.magic.academy.core.hotbar.LobbyHotbarManager.PDC_NAMESPACE,
                gg.magic.academy.core.hotbar.LobbyHotbarManager.PDC_ACTION_KEY);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        var item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
        if ("hideout".equals(action)) {
            event.setCancelled(true);
            hideoutManager.visitHideout(player);
        }
    }
}
