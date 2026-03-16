package gg.magic.academy.spells.upgrade;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.api.spell.SpellTier;
import gg.magic.academy.items.MagicItems;
import gg.magic.academy.items.registry.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

public class SpellUpgradeManager {

    private final JavaPlugin plugin;

    public SpellUpgradeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempt to upgrade the player's spell to the next tier.
     * Consumes required items from inventory.
     * Returns true on success, false if requirements not met.
     */
    public boolean upgrade(Player player, MagicPlayerData data, SpellTemplate spell) {
        int currentTier = data.getSpellTier(spell.id());
        int nextTier = currentTier + 1;

        if (nextTier > spell.maxTier()) {
            player.sendMessage(Component.text("✦ " + spell.name() + " is already at maximum tier.")
                    .color(TextColor.color(0xAAAAAA)));
            return false;
        }

        Optional<SpellTier> tierOpt = spell.tier(nextTier);
        if (tierOpt.isEmpty()) return false;

        SpellTier tier = tierOpt.get();
        Map<String, Integer> cost = tier.upgradeCost();

        // Verify player has all required items
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            if (!hasEnough(player, entry.getKey(), entry.getValue())) {
                player.sendMessage(Component.text("✦ You need " + entry.getValue() + "x " + entry.getKey()
                        + " to upgrade " + spell.name() + " to Tier " + nextTier + ".")
                        .color(TextColor.color(0xFF5555)));
                return false;
            }
        }

        // Consume items
        for (Map.Entry<String, Integer> entry : cost.entrySet()) {
            consumeItems(player, entry.getKey(), entry.getValue());
        }

        // Apply upgrade
        data.setSpellTier(spell.id(), nextTier);
        player.sendMessage(Component.text("✦ " + spell.name() + " upgraded to Tier " + nextTier + "!")
                .color(TextColor.color(0x55FF55)));
        return true;
    }

    private boolean hasEnough(Player player, String itemId, int required) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null) continue;
            if (itemId.equals(ItemRegistry.getItemId(stack))) {
                count += stack.getAmount();
            }
        }
        return count >= required;
    }

    private void consumeItems(Player player, String itemId, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || remaining <= 0) continue;
            if (!itemId.equals(ItemRegistry.getItemId(stack))) continue;
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
        }
    }
}
