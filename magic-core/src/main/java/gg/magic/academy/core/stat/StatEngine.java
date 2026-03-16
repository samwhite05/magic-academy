package gg.magic.academy.core.stat;

import gg.magic.academy.api.player.MagicPlayerData;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes final stat values for a player by aggregating:
 * - Base stats
 * - Hideout module bonuses (injected by magic-hideouts via StatModifier)
 * - Artifact bonuses (injected similarly)
 *
 * Other modules register StatModifiers here instead of directly modifying player data.
 */
public class StatEngine {

    private final Map<String, StatModifier> modifiers = new HashMap<>();

    public void registerModifier(String id, StatModifier modifier) {
        modifiers.put(id, modifier);
    }

    public void unregisterModifier(String id) {
        modifiers.remove(id);
    }

    /**
     * Compute final max mana for the player (base + all bonuses).
     */
    public int computeMaxMana(MagicPlayerData data) {
        int base = data.getMaxMana();
        int bonus = 0;
        for (StatModifier mod : modifiers.values()) {
            bonus += mod.getMaxManaBonus(data);
        }
        return base + bonus;
    }

    /**
     * Compute final spell damage multiplier (1.0 = no bonus).
     */
    public double computeSpellDamageMultiplier(MagicPlayerData data) {
        double mult = 1.0;
        for (StatModifier mod : modifiers.values()) {
            mult += mod.getSpellDamageBonus(data);
        }
        return mult;
    }

    /**
     * Compute mana regen per second bonus.
     */
    public int computeManaRegenBonus(MagicPlayerData data) {
        int bonus = 0;
        for (StatModifier mod : modifiers.values()) {
            bonus += mod.getManaRegenBonus(data);
        }
        return bonus;
    }
}
