package gg.magic.academy.contract;

import gg.magic.academy.api.Rarity;
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
import java.util.Map;

/**
 * Paginated GUI showing available / active / completed contracts.
 * Players click an AVAILABLE contract to accept it.
 */
public class ContractBoardMenu {

    public static final String TITLE = "✦ Contract Board";
    private static final int CONTRACTS_PER_PAGE = 45;

    private final JavaPlugin plugin;
    private final ContractManager contractManager;
    public final NamespacedKey contractKey;
    public final NamespacedKey pageKey;

    public ContractBoardMenu(JavaPlugin plugin, ContractManager contractManager) {
        this.plugin = plugin;
        this.contractManager = contractManager;
        this.contractKey = new NamespacedKey(plugin, "contract_id");
        this.pageKey = new NamespacedKey(plugin, "contract_page");
    }

    public void open(Player player, int page) {
        List<ContractTemplate> all = new ArrayList<>(contractManager.getRegistry().getAll());
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) CONTRACTS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE));

        int start = page * CONTRACTS_PER_PAGE;
        int end = Math.min(start + CONTRACTS_PER_PAGE, all.size());

        for (int i = start; i < end; i++) {
            ContractTemplate t = all.get(i);
            ContractState state = getState(player, t.id());
            int[] prog = contractManager.getProgress(player.getUniqueId())
                    .getOrDefault(t.id(), new int[0]);
            inv.setItem(i - start, buildItem(t, state, prog));
        }

        // Bottom row
        ItemStack filler = buildGlass();
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);
        if (page > 0)           inv.setItem(45, buildNav("◀ Previous", page - 1));
        inv.setItem(49, buildInfoItem(page, totalPages));
        if (page < totalPages - 1) inv.setItem(53, buildNav("Next ▶", page + 1));

        player.openInventory(inv);
    }

    private ContractState getState(Player player, String contractId) {
        if (contractManager.isActive(player.getUniqueId(), contractId)) return ContractState.ACTIVE;
        if (contractManager.isCompleted(player.getUniqueId(), contractId)) return ContractState.COMPLETED;
        return ContractState.AVAILABLE;
    }

    private ItemStack buildItem(ContractTemplate t, ContractState state, int[] prog) {
        Material mat = rarityMaterial(t.rarity());
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();

        TextColor nameColor = switch (state) {
            case AVAILABLE -> t.rarity().color();
            case ACTIVE    -> TextColor.color(0xFFFF55);
            case COMPLETED -> TextColor.color(0x555555);
        };

        meta.displayName(Component.text("[" + state.label + "] " + t.name())
                .color(nameColor).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (!t.description().isBlank()) {
            lore.add(Component.text(t.description()).color(TextColor.color(0xCCCCCC))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Rarity: " + t.rarity().name()).color(t.rarity().color())
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Objectives:").color(TextColor.color(0xAAAAAA))
                .decoration(TextDecoration.ITALIC, false));

        for (int i = 0; i < t.objectives().size(); i++) {
            ContractObjective obj = t.objectives().get(i);
            int current = (prog != null && i < prog.length) ? prog[i] : 0;
            boolean done = current >= obj.required();
            lore.add(Component.text("  " + (done ? "✔" : "○") + " " + obj.label()
                    + " (" + current + "/" + obj.required() + ")")
                    .color(done ? TextColor.color(0x55FF55) : TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (!t.rewardItems().isEmpty() || t.rewardMana() > 0) {
            lore.add(Component.empty());
            lore.add(Component.text("Rewards:").color(TextColor.color(0xAAAAAA))
                    .decoration(TextDecoration.ITALIC, false));
            for (Map.Entry<String, Integer> e : t.rewardItems().entrySet()) {
                lore.add(Component.text("  + " + e.getValue() + "x " + e.getKey())
                        .color(TextColor.color(0xFFAA00)).decoration(TextDecoration.ITALIC, false));
            }
            if (t.rewardMana() > 0) {
                lore.add(Component.text("  + " + t.rewardMana() + " mana")
                        .color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            }
        }

        if (state == ContractState.AVAILABLE) {
            lore.add(Component.empty());
            lore.add(Component.text("Click to accept").color(TextColor.color(0x00FF99))
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.getPersistentDataContainer().set(contractKey, PersistentDataType.STRING, t.id());
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildNav(String label, int targetPage) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(label).color(TextColor.color(0x00FF99))
                .decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, targetPage);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildInfoItem(int page, int total) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Contract Board").color(TextColor.color(0xFFAA00))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Page " + (page + 1) + " / " + total)
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false)));
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

    private Material rarityMaterial(Rarity rarity) {
        return switch (rarity) {
            case LEGENDARY -> Material.DRAGON_EGG;
            case EPIC      -> Material.PURPUR_BLOCK;
            case RARE      -> Material.LAPIS_LAZULI;
            default        -> Material.PAPER;
        };
    }

    private enum ContractState {
        AVAILABLE("Open"),
        ACTIVE("Active"),
        COMPLETED("Done");

        final String label;
        ContractState(String label) { this.label = label; }
    }
}
