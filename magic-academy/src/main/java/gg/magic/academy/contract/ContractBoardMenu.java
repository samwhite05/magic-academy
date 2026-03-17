package gg.magic.academy.contract;

import gg.magic.academy.api.Rarity;
import gg.magic.academy.api.gui.GuiUtil;
import gg.magic.academy.api.gui.PagedGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContractBoardMenu {

    public static final String TITLE = "✦ Contract Board";

    private final ContractManager contractManager;

    public ContractBoardMenu(ContractManager contractManager) {
        this.contractManager = contractManager;
    }

    public void open(Player player, int page) {
        List<ContractTemplate> all = new ArrayList<>(contractManager.getRegistry().getAll());

        PagedGui gui = new PagedGui(Component.text(TITLE));

        // Info item at slot 40 (centre of nav row)
        gui.setFixed(40, GuiUtil.make(Material.PAPER,
                Component.text("Contract Board")
                        .color(TextColor.color(0xFFAA00)).decoration(TextDecoration.ITALIC, false),
                List.of(
                        Component.text("Click available contracts to accept")
                                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false)
                )));

        for (ContractTemplate t : all) {
            ContractState state = getState(player, t.id());
            int[] prog = contractManager.getProgress(player.getUniqueId())
                    .getOrDefault(t.id(), new int[0]);
            gui.addItem(buildContractItem(t, state, prog), e -> {
                if (state == ContractState.AVAILABLE && e.getWhoClicked() instanceof Player p) {
                    contractManager.startContract(p, t.id());
                    p.sendMessage(Component.text("✦ Contract accepted: " + t.name())
                            .color(TextColor.color(0x55FF55)));
                    open(p, 0);
                }
            });
        }

        gui.open(player, page);
    }

    private ContractState getState(Player player, String contractId) {
        if (contractManager.isActive(player.getUniqueId(), contractId)) return ContractState.ACTIVE;
        if (contractManager.isCompleted(player.getUniqueId(), contractId)) return ContractState.COMPLETED;
        return ContractState.AVAILABLE;
    }

    private ItemStack buildContractItem(ContractTemplate t, ContractState state, int[] prog) {
        Material mat = rarityMaterial(t.rarity());

        TextColor nameColor = switch (state) {
            case AVAILABLE -> t.rarity().color();
            case ACTIVE    -> TextColor.color(0xFFFF55);
            case COMPLETED -> TextColor.color(0x555555);
        };

        List<Component> lore = new ArrayList<>();
        if (!t.description().isBlank()) {
            lore.add(Component.text(t.description())
                    .color(TextColor.color(0xCCCCCC)).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Rarity: " + t.rarity().name())
                .color(t.rarity().color()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Objectives:")
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));

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
            lore.add(Component.text("Rewards:")
                    .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
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
            lore.add(Component.text("Click to accept")
                    .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        }

        return GuiUtil.make(mat,
                Component.text("[" + state.label + "] " + t.name())
                        .color(nameColor).decoration(TextDecoration.ITALIC, false),
                lore);
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
