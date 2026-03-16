package gg.magic.academy.spells.loadout;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpellLoadoutListener implements Listener {

    private static final String MENU_TITLE = "Spell Loadout";

    private final SpellLoadoutMenu menu;
    private final Map<UUID, Integer> selectedSlots = new ConcurrentHashMap<>();

    public SpellLoadoutListener(SpellLoadoutMenu menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isMenu(event)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() == null) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        int slot = event.getRawSlot();
        if (slot <= 3) {
            if (event.isRightClick()) {
                data.clearSlot(slot);
                menu.open(player, slot);
                return;
            }
            selectedSlots.put(player.getUniqueId(), slot);
            menu.open(player, slot);
            return;
        }

        if (slot < 9) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String spellId = meta.getPersistentDataContainer()
                .get(menu.getSpellIdKey(), PersistentDataType.STRING);
        if (spellId == null || spellId.isBlank()) return;

        int target = selectedSlots.getOrDefault(player.getUniqueId(), 0);
        if (!data.equipSpell(target, spellId)) return;

        menu.open(player, target);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isMenu(event)) return;
        selectedSlots.remove(player.getUniqueId());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isMenu(event)) return;
        event.setCancelled(true);
    }

    private boolean isMenu(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        return MENU_TITLE.equals(title);
    }

    private boolean isMenu(InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        return MENU_TITLE.equals(title);
    }

    private boolean isMenu(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        return MENU_TITLE.equals(title);
    }
}

