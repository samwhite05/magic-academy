package gg.magic.academy.api.spell;

import gg.magic.academy.api.element.Element;

import java.util.List;
import java.util.Optional;

/**
 * The static definition of a spell loaded from YAML.
 * Immutable — all player-specific state (current tier, cooldowns) lives in MagicPlayerData.
 */
public record SpellTemplate(
        String id,
        String name,
        Element element,
        SpellShape shape,
        SpellEffect effect,
        List<SpellTier> tiers
) {
    /** Returns the tier config for a given tier number (1-based). */
    public Optional<SpellTier> tier(int tierNumber) {
        return tiers.stream().filter(t -> t.tier() == tierNumber).findFirst();
    }

    public int maxTier() {
        return tiers.stream().mapToInt(SpellTier::tier).max().orElse(1);
    }
}
