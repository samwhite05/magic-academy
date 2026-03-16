package gg.magic.academy.core.stat;

import gg.magic.academy.api.player.MagicPlayerData;

/**
 * Implemented by other modules (magic-hideouts, magic-artifacts, etc.) to inject
 * stat bonuses into the StatEngine without creating hard dependencies.
 */
public interface StatModifier {
    default int getMaxManaBonus(MagicPlayerData data) { return 0; }
    default double getSpellDamageBonus(MagicPlayerData data) { return 0.0; }
    default int getManaRegenBonus(MagicPlayerData data) { return 0; }
}
