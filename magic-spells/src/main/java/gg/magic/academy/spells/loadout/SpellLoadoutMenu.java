package gg.magic.academy.spells.loadout;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.api.spell.SpellTier;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.items.MagicItems;
import gg.magic.academy.spells.registry.SpellRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI menu for viewing and equipping spell loadout slots.
 * Top row: 4 loadout slots (current equipped spells).
 * Remaining slots: all known spells (click to equip in selected slot).
 */
public class SpellLoadoutMenu {

    private static final String MENU_TITLE = "Spell Loadout";
    private final JavaPlugin plugin;
    private final SpellRegistry spellRegistry;
    private final NamespacedKey spellIdKey;

    public SpellLoadoutMenu(JavaPlugin plugin, SpellRegistry spellRegistry) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.spellIdKey = new NamespacedKey(plugin, "spell_id");
    }

    public void open(Player player) {
        open(player, -1);
    }

    public void open(Player player, int selectedSlot) {
        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(MENU_TITLE));

        ItemStack bg = uiItem("ui_spellbook_bg", Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, bg.clone());
        }

        // Slots 0-3: current loadout
        List<String> equipped = data.getEquippedSpells();
        for (int i = 0; i < 4; i++) {
            String spellId = equipped.get(i);
            if (spellId != null) {
                int slotIndex = i;
                spellRegistry.get(spellId).ifPresent(spell -> {
                    inv.setItem(slotIndex, buildSpellItem(spell, data.getSpellTier(spell.id()), true, selectedSlot == slotIndex));
                });
            } else {
                inv.setItem(i, buildEmptySlot(i, selectedSlot == i));
            }
        }

        // Separator
        ItemStack divider = uiItem("ui_spellbook_divider", Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 4; i < 9; i++) inv.setItem(i, divider.clone());

        // Known spells list starting at slot 9
        int slot = 9;
        for (SpellTemplate spell : spellRegistry.getAll()) {
            if (!data.hasSpell(spell.id())) continue;
            if (slot >= 54) break;
            inv.setItem(slot++, buildSpellItem(spell, data.getSpellTier(spell.id()), false, false));
        }

        player.openInventory(inv);
    }

    private ItemStack buildSpellItem(SpellTemplate spell, int currentTier, boolean equipped, boolean selected) {
        Material mat = elementToMaterial(spell.element().name());
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        TextColor color = equipped ? TextColor.color(0x55FF55) : TextColor.color(0xFFAA00);
        meta.displayName(Component.text((equipped ? "* " : "") + spell.name() + " [T" + currentTier + "]")
                .color(color).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Element: " + spell.element().name()).color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shape: " + spell.shape().name()).color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Effect: " + spell.effect().name()).color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));

        spell.tier(currentTier).ifPresent(t -> {
            lore.add(Component.empty());
            lore.add(Component.text(t.description()).color(TextColor.color(0xCCCCCC)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Mana: " + t.manaCost()).color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cooldown: " + (t.cooldownMs() / 1000.0) + "s").color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));
        });

        if (selected) {
            lore.add(Component.empty());
            lore.add(Component.text("Selected Slot").color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        }

        if (!equipped) {
            lore.add(Component.empty());
            lore.add(Component.text("Click to equip").color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        }

        meta.getPersistentDataContainer().set(spellIdKey, PersistentDataType.STRING, spell.id());
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildEmptySlot(int slot, boolean selected) {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (selected) {
            meta.lore(List.of(Component.text("Selected Slot")
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false)));
        }
        meta.displayName(Component.text("Slot " + (slot + 1) + " - Empty")
                .color(TextColor.color(0x666666)).decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildGlass() {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack uiItem(String id, Material fallback) {
        MagicItems items = MagicItems.get();
        if (items != null) {
            var opt = items.getItemRegistry().get(id);
            if (opt.isPresent()) return opt.get();
        }
        ItemStack stack = new ItemStack(fallback);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }

    private Material elementToMaterial(String element) {
        return switch (element) {
            case "FIRE" -> Material.BLAZE_POWDER;
            case "ICE" -> Material.PACKED_ICE;
            case "LIGHTNING" -> Material.LIGHTNING_ROD;
            case "SHADOW" -> Material.COAL;
            default -> Material.AMETHYST_SHARD;
        };
    }

    public NamespacedKey getSpellIdKey() {
        return spellIdKey;
    }
}


