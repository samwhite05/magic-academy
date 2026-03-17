package gg.magic.academy.spells.loadout;

import gg.magic.academy.api.gui.GuiUtil;
import gg.magic.academy.api.gui.SimpleGui;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.spells.registry.SpellRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpellLoadoutMenu {

    private static final Component TITLE = Component.text("Spell Loadout");
    private final SpellRegistry spellRegistry;

    public SpellLoadoutMenu(SpellRegistry spellRegistry) {
        this.spellRegistry = spellRegistry;
    }

    public void open(Player player) {
        open(player, -1);
    }

    public void open(Player player, int selectedSlot) {
        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;

        SimpleGui gui = new SimpleGui(6, TITLE);

        // Background
        ItemStack black = GuiUtil.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) gui.set(i, black);

        // Equipped spell slots (0-3)
        List<String> equipped = data.getEquippedSpells();
        for (int i = 0; i < 4; i++) {
            String spellId = equipped.get(i);
            final int slotIndex = i;
            if (spellId != null) {
                spellRegistry.get(spellId).ifPresent(spell ->
                        gui.set(slotIndex, buildSpellItem(spell, data.getSpellTier(spell.id()), true, selectedSlot == slotIndex)));
            } else {
                gui.set(i, buildEmptySlot(i, selectedSlot == i));
            }
        }

        // Separator (slots 4-8)
        ItemStack gray = GuiUtil.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 4; i < 9; i++) gui.set(i, gray);

        // Discovered spells (slots 9+) — display only, no click action
        int slot = 9;
        for (SpellTemplate spell : spellRegistry.getAll()) {
            if (!data.hasSpell(spell.id())) continue;
            if (slot >= 54) break;
            gui.set(slot, buildSpellItem(spell, data.getSpellTier(spell.id()), false, false));
            slot++;
        }

        gui.open(player);
    }

    private ItemStack buildSpellItem(SpellTemplate spell, int currentTier, boolean equipped, boolean selected) {
        TextColor color = equipped ? TextColor.color(0x55FF55) : TextColor.color(0xFFAA00);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Element: " + spell.element().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shape: " + spell.shape().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Effect: " + spell.effect().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));

        spell.tier(currentTier).ifPresent(t -> {
            lore.add(Component.empty());
            lore.add(Component.text(t.description())
                    .color(TextColor.color(0xCCCCCC)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Mana: " + t.manaCost())
                    .color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cooldown: " + (t.cooldownMs() / 1000.0) + "s")
                    .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));
        });

        if (selected) {
            lore.add(Component.empty());
            lore.add(Component.text("Selected Slot")
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        }
        if (!equipped) {
            lore.add(Component.empty());
            lore.add(Component.text("Click to equip")
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        }

        return GuiUtil.make(Material.BOOK, getSpellCmd(currentTier),
                Component.text((equipped ? "* " : "") + spell.name() + " [T" + currentTier + "]")
                        .color(color).decoration(TextDecoration.ITALIC, false),
                lore);
    }

    private ItemStack buildEmptySlot(int slot, boolean selected) {
        List<Component> lore = new ArrayList<>();
        if (selected) {
            lore.add(Component.text("Selected Slot")
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        }
        return GuiUtil.make(Material.GRAY_STAINED_GLASS_PANE,
                Component.text("Slot " + (slot + 1) + " - Empty")
                        .color(TextColor.color(0x666666)).decoration(TextDecoration.ITALIC, false),
                lore);
    }

    /** Maps spell tier (1-4) to the resource pack custom_model_data for spell_tier_N items. */
    private int getSpellCmd(int tier) {
        return switch (tier) {
            case 1 -> 4001;
            case 2 -> 4002;
            case 3 -> 4003;
            case 4 -> 4004;
            default -> 4001;
        };
    }

    private void equipSpell(Player player, String spellId, int slot) {
        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;
        data.equipSpell(slot, spellId);
        player.sendMessage(Component.text("✦ Equipped " + spellId + " to slot " + (slot + 1))
                .color(TextColor.color(0x55FF55)));
        open(player);
    }
}
