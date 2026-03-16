package gg.magic.academy.hideouts.module;

import java.util.Map;

/**
 * A single upgrade tier for a hideout module.
 */
public record ModuleTier(
        int tier,
        String description,
        Map<String, Integer> cost,     // itemId -> amount
        int maxManaBonus,
        double spellDamageBonus,
        int manaRegenBonus
) {}
