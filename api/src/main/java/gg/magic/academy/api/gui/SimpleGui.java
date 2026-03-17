package gg.magic.academy.api.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SimpleGui implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    public SimpleGui(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public void set(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    public void set(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) handlers.put(slot, handler);
    }

    public void set(int slot, ItemStack item, Runnable handler) {
        set(slot, item, handler != null ? e -> handler.run() : null);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= inventory.getSize()) return;
        Consumer<InventoryClickEvent> handler = handlers.get(event.getRawSlot());
        if (handler != null) handler.accept(event);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
