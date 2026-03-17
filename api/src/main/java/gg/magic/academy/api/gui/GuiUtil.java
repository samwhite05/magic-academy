package gg.magic.academy.api.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class GuiUtil {

    private GuiUtil() {}

    public static ItemStack make(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack make(Material material, Component name, List<Component> lore) {
        ItemStack item = make(material, name);
        ItemMeta meta = item.getItemMeta();
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack make(Material material, int customModelData, Component name, List<Component> lore) {
        ItemStack item = make(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler(Material material) {
        return make(material, Component.text(" ").decoration(TextDecoration.ITALIC, false));
    }

    public static ItemStack filler() {
        return filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** Add enchantment glow to an item using Paper's native API. */
    public static ItemStack glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }
}
