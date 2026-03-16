package gg.magic.academy.contract;

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

public class ContractBoardListener implements Listener {

    private final ContractBoardMenu menu;
    private final ContractManager contractManager;
    private final Map<UUID, Integer> currentPage = new ConcurrentHashMap<>();

    public ContractBoardListener(ContractBoardMenu menu, ContractManager contractManager) {
        this.menu = menu;
        this.contractManager = contractManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMenu(event)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        var clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        // Page nav?
        Integer targetPage = meta.getPersistentDataContainer().get(menu.pageKey, PersistentDataType.INTEGER);
        if (targetPage != null) {
            currentPage.put(player.getUniqueId(), targetPage);
            menu.open(player, targetPage);
            return;
        }

        // Contract click?
        String contractId = meta.getPersistentDataContainer().get(menu.contractKey, PersistentDataType.STRING);
        if (contractId == null) return;

        contractManager.startContract(player, contractId);
        menu.open(player, currentPage.getOrDefault(player.getUniqueId(), 0));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isMenu(event)) return;
        event.setCancelled(true);
    }

    private boolean isMenu(InventoryClickEvent event) {
        return ContractBoardMenu.TITLE.equals(
                PlainTextComponentSerializer.plainText().serialize(event.getView().title()));
    }

    private boolean isMenu(InventoryDragEvent event) {
        return ContractBoardMenu.TITLE.equals(
                PlainTextComponentSerializer.plainText().serialize(event.getView().title()));
    }
}
