package gg.magic.academy.hideouts.menu;

import gg.magic.academy.api.gui.GuiUtil;
import gg.magic.academy.api.gui.SimpleGui;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.hideouts.module.HideoutModule;
import gg.magic.academy.hideouts.module.ModuleRegistry;
import gg.magic.academy.hideouts.module.ModuleTier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleUpgradeMenu {

    private final ModuleRegistry moduleRegistry;

    public ModuleUpgradeMenu(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    public void open(Player player) {
        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;

        SimpleGui gui = new SimpleGui(3, Component.text("Hideout Upgrades"));

        int slot = 10;
        for (HideoutModule module : moduleRegistry.getAll()) {
            if (slot >= 18) break;
            int currentTier = data.getModuleLevel(module.id());
            gui.set(slot, GuiUtil.make(moduleMaterial(module.id()),
                    Component.text(module.name() + " [T" + currentTier + "]")
                            .color(TextColor.color(0x55FFAA)).decoration(TextDecoration.ITALIC, false),
                    buildLore(module, currentTier)),
                    e -> {
                        if (e.getWhoClicked() instanceof Player p)
                            handleUpgradeClick(p, data, module, currentTier);
                    });
            slot++;
            if (slot == 13) slot = 14;
        }

        gui.open(player);
    }

    private List<Component> buildLore(HideoutModule module, int currentTier) {
        List<Component> lore = new ArrayList<>();

        if (!module.description().isBlank()) {
            lore.add(Component.text(module.description())
                    .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Max Tier: " + module.maxTier())
                .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));

        int nextTier = currentTier + 1;
        Optional<ModuleTier> tierOpt = module.tier(nextTier);
        if (tierOpt.isPresent()) {
            ModuleTier tier = tierOpt.get();
            lore.add(Component.empty());
            lore.add(Component.text("Next Tier " + nextTier)
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
            if (!tier.description().isBlank()) {
                lore.add(Component.text(tier.description())
                        .color(TextColor.color(0xCCCCCC)).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Max Mana +" + tier.maxManaBonus())
                    .color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Spell Damage +" + (int) (tier.spellDamageBonus() * 100) + "%")
                    .color(TextColor.color(0xFFAA55)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Mana Regen +" + tier.manaRegenBonus())
                    .color(TextColor.color(0x66FFCC)).decoration(TextDecoration.ITALIC, false));

            if (!tier.cost().isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Cost:")
                        .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
                for (var entry : tier.cost().entrySet()) {
                    lore.add(Component.text("- " + entry.getValue() + "x " + entry.getKey())
                            .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
                }
            }

            lore.add(Component.empty());
            lore.add(Component.text("Click to upgrade")
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("Max tier reached")
                    .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    private void handleUpgradeClick(Player player, MagicPlayerData data, HideoutModule module, int currentTier) {
        int nextTier = currentTier + 1;
        if (nextTier > module.maxTier()) {
            player.sendMessage(Component.text("✦ This module is already at max tier!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }
        if (module.tier(nextTier).isEmpty()) {
            player.sendMessage(Component.text("✦ No more tiers available.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }
        player.sendMessage(Component.text("✦ Upgrade functionality coming soon!")
                .color(TextColor.color(0xFFAA00)));
    }

    private Material moduleMaterial(String moduleId) {
        String id = moduleId.toLowerCase();
        if (id.contains("alchemy")) return Material.BREWING_STAND;
        if (id.contains("rune")) return Material.ANVIL;
        if (id.contains("artifact")) return Material.ITEM_FRAME;
        if (id.contains("spell")) return Material.ENCHANTING_TABLE;
        return Material.BOOKSHELF;
    }
}
