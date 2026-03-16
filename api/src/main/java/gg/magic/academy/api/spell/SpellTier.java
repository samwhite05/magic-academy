package gg.magic.academy.api.spell;

import java.util.Map;

/**
 * A single tier of a spell — its stats, MythicMobs skill reference, and upgrade cost.
 */
public record SpellTier(
        int tier,
        String description,
        int manaCost,
        long cooldownMs,
        String mythicSkillId,
        Map<String, Integer> upgradeCost   // itemId -> amount required to upgrade TO this tier
) {}
