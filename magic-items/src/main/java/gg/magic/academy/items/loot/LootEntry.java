package gg.magic.academy.items.loot;

/**
 * A single entry in a loot table.
 */
public record LootEntry(
        String itemId,
        int minAmount,
        int maxAmount,
        double weight   // relative weight for weighted random selection
) {}
