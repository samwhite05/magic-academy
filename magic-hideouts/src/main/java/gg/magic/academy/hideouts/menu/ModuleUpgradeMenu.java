package gg.magic.academy.hideouts.menu;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.hideouts.module.HideoutModule;
import gg.magic.academy.hideouts.module.ModuleRegistry;
import gg.magic.academy.hideouts.module.ModuleTier;
import gg.magic.academy.items.MagicItems;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleUpgradeMenu {

    public static final String MENU_TITLE = "Hideout Upgrades";

    private final JavaPlugin plugin;
    private final ModuleRegistry moduleRegistry;
    private final NamespacedKey moduleIdKey;

    public ModuleUpgradeMenu(JavaPlugin plugin, ModuleRegistry moduleRegistry) {
        this.plugin = plugin;
        this.moduleRegistry = moduleRegistry;
        this.moduleIdKey = new NamespacedKey(plugin, "module_id");
    }

    public void open(Player player) {
        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, Component.text(MENU_TITLE));

        ItemStack bg = uiItem("ui_hideout_bg", Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, bg.clone());
        }

        int slot = 10;
        for (HideoutModule module : moduleRegistry.getAll()) {
            if (slot >= inv.getSize()) break;
            int currentTier = data.getModuleLevel(module.id());
            inv.setItem(slot, buildModuleItem(module, currentTier));
            slot++;
            if (slot == 13) slot = 14; // small spacing
        }

        player.openInventory(inv);
    }

    private ItemStack buildModuleItem(HideoutModule module, int currentTier) {
        ItemStack stack = new ItemStack(moduleMaterial(module.id()));
        ItemMeta meta = stack.getItemMeta();

        meta.displayName(Component.text(module.name() + " [T" + currentTier + "]")
                .color(TextColor.color(0x55FFAA)).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (!module.description().isBlank()) {
            lore.add(Component.text(module.description()).color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Max Tier: " + module.maxTier()).color(TextColor.color(0x888888))
                .decoration(TextDecoration.ITALIC, false));

        int nextTier = currentTier + 1;
        Optional<ModuleTier> tierOpt = module.tier(nextTier);
        if (tierOpt.isPresent()) {
            ModuleTier tier = tierOpt.get();
            lore.add(Component.empty());
            lore.add(Component.text("Next Tier " + nextTier).color(TextColor.color(0x00FF99))
                    .decoration(TextDecoration.ITALIC, false));
            if (!tier.description().isBlank()) {
                lore.add(Component.text(tier.description()).color(TextColor.color(0xCCCCCC))
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Max Mana +" + tier.maxManaBonus())
                    .color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Spell Damage +" + (int) (tier.spellDamageBonus() * 100) + "%")
                    .color(TextColor.color(0xFFAA55)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Mana Regen +" + tier.manaRegenBonus())
                    .color(TextColor.color(0x66FFCC)).decoration(TextDecoration.ITALIC, false));

            if (!tier.cost().isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Cost:").color(TextColor.color(0xAAAAAA))
                        .decoration(TextDecoration.ITALIC, false));
                for (var entry : tier.cost().entrySet()) {
                    lore.add(Component.text("- " + entry.getValue() + "x " + entry.getKey())
                            .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
                }
            }

            lore.add(Component.empty());
            lore.add(Component.text("Click to upgrade").color(TextColor.color(0x00FF99))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("Max tier reached").color(TextColor.color(0x888888))
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.getPersistentDataContainer().set(moduleIdKey, PersistentDataType.STRING, module.id());
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private Material moduleMaterial(String moduleId) {
        String id = moduleId.toLowerCase();
        if (id.contains("alchemy")) return Material.BREWING_STAND;
        if (id.contains("rune")) return Material.ANVIL;
        if (id.contains("artifact")) return Material.ITEM_FRAME;
        if (id.contains("spell")) return Material.ENCHANTING_TABLE;
        return Material.BOOKSHELF;
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

    public NamespacedKey getModuleIdKey() {
        return moduleIdKey;
    }
}

