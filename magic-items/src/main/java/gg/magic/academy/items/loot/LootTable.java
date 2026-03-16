package gg.magic.academy.items.loot;

import gg.magic.academy.items.registry.ItemRegistry;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * A weighted loot table. Call roll() to get a random selection of items.
 */
public class LootTable {

    private final String id;
    private final List<LootEntry> entries;
    private final int rolls;   // how many picks per roll
    private final double guaranteedChance; // 0.0-1.0 chance each roll produces an item

    public LootTable(String id, List<LootEntry> entries, int rolls, double guaranteedChance) {
        this.id = id;
        this.entries = List.copyOf(entries);
        this.rolls = rolls;
        this.guaranteedChance = guaranteedChance;
    }

    /**
     * Rolls this loot table and returns the resulting items.
     */
    public List<ItemStack> roll(ItemRegistry registry, Random rng) {
        List<ItemStack> result = new ArrayList<>();
        double totalWeight = entries.stream().mapToDouble(LootEntry::weight).sum();

        for (int i = 0; i < rolls; i++) {
            if (rng.nextDouble() > guaranteedChance) continue;

            double pick = rng.nextDouble() * totalWeight;
            double cumulative = 0;
            for (LootEntry entry : entries) {
                cumulative += entry.weight();
                if (pick <= cumulative) {
                    int amount = entry.minAmount() + rng.nextInt(entry.maxAmount() - entry.minAmount() + 1);
                    registry.get(entry.itemId(), amount).ifPresent(result::add);
                    break;
                }
            }
        }
        return result;
    }

    public String getId() { return id; }
    public List<LootEntry> getEntries() { return entries; }
}
