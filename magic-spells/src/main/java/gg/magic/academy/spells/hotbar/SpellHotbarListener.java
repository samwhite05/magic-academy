package gg.magic.academy.spells.hotbar;

import gg.magic.academy.core.MagicCore;
import gg.magic.academy.spells.discovery.DiscoveriesMenu;
import gg.magic.academy.spells.loadout.SpellLoadoutMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class SpellHotbarListener implements Listener {

    private final SpellLoadoutMenu loadoutMenu;
    private final DiscoveriesMenu discoveriesMenu;
    private final NamespacedKey actionKey;

    public SpellHotbarListener(SpellLoadoutMenu loadoutMenu, DiscoveriesMenu discoveriesMenu) {
        this.loadoutMenu = loadoutMenu;
        this.discoveriesMenu = discoveriesMenu;
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
        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "spellbook"    -> { event.setCancelled(true); loadoutMenu.open(player); }
            case "discoveries"  -> { event.setCancelled(true); discoveriesMenu.open(player, 0); }
        }
    }
}
