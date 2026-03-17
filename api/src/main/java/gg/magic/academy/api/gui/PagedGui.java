package gg.magic.academy.api.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 5-row paginated GUI. Content fills slots 0-35 (4 rows); the bottom row
 * (slots 36-44) is reserved for navigation and fixed info items.
 */
public class PagedGui {

    private static final int PAGE_SIZE = 36;
    private static final int NAV_START = 36;

    private final Component title;
    private final List<ItemStack> items = new ArrayList<>();
    private final List<Consumer<InventoryClickEvent>> itemHandlers = new ArrayList<>();
    private final Map<Integer, ItemStack> fixedItems = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> fixedHandlers = new HashMap<>();

    public PagedGui(Component title) {
        this.title = title;
    }

    public void addItem(ItemStack item, Consumer<InventoryClickEvent> handler) {
        items.add(item);
        itemHandlers.add(handler);
    }

    public void addItem(ItemStack item) {
        addItem(item, null);
    }

    /**
     * Place a fixed item in the navigation row by absolute slot (36-44).
     * Use slot 40 for a centred info item.
     */
    public void setFixed(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        fixedItems.put(slot, item);
        if (handler != null) fixedHandlers.put(slot, handler);
    }

    public void setFixed(int slot, ItemStack item) {
        setFixed(slot, item, null);
    }

    public void open(Player player, int page) {
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        final int finalPage = page;

        SimpleGui gui = new SimpleGui(5, title);

        // Fill nav row
        ItemStack filler = GuiUtil.filler();
        for (int i = NAV_START; i < NAV_START + 9; i++) gui.set(i, filler);

        // Fixed items
        for (var entry : fixedItems.entrySet()) {
            gui.set(entry.getKey(), entry.getValue(), fixedHandlers.get(entry.getKey()));
        }

        // Prev / Next
        if (page > 0) {
            gui.set(NAV_START, GuiUtil.make(Material.ARROW,
                    Component.text("◀ Previous").color(TextColor.color(0xFFFFFF))
                            .decoration(TextDecoration.ITALIC, false)),
                    e -> open((Player) e.getWhoClicked(), finalPage - 1));
        }
        if (page < totalPages - 1) {
            gui.set(NAV_START + 8, GuiUtil.make(Material.ARROW,
                    Component.text("Next ▶").color(TextColor.color(0xFFFFFF))
                            .decoration(TextDecoration.ITALIC, false)),
                    e -> open((Player) e.getWhoClicked(), finalPage + 1));
        }

        // Content
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());
        for (int i = start; i < end; i++) {
            gui.set(i - start, items.get(i), itemHandlers.get(i));
        }

        gui.open(player);
    }
}
