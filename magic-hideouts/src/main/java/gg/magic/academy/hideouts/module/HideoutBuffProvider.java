package gg.magic.academy.hideouts.module;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.stat.StatModifier;

/**
 * Injects hideout module passive bonuses into the StatEngine.
 */
public class HideoutBuffProvider implements StatModifier {

    private final ModuleRegistry registry;

    public HideoutBuffProvider(ModuleRegistry registry) {
        this.registry = registry;
    }

    @Override
    public int getMaxManaBonus(MagicPlayerData data) {
        int bonus = 0;
        for (var entry : data.getModuleLevels().entrySet()) {
            int level = entry.getValue();
            if (level <= 0) continue;
            bonus += registry.get(entry.getKey())
                    .flatMap(m -> m.tier(level))
                    .map(ModuleTier::maxManaBonus)
                    .orElse(0);
        }
        return bonus;
    }

    @Override
    public double getSpellDamageBonus(MagicPlayerData data) {
        double bonus = 0.0;
        for (var entry : data.getModuleLevels().entrySet()) {
            int level = entry.getValue();
            if (level <= 0) continue;
            bonus += registry.get(entry.getKey())
                    .flatMap(m -> m.tier(level))
                    .map(ModuleTier::spellDamageBonus)
                    .orElse(0.0);
        }
        return bonus;
    }

    @Override
    public int getManaRegenBonus(MagicPlayerData data) {
        int bonus = 0;
        for (var entry : data.getModuleLevels().entrySet()) {
            int level = entry.getValue();
            if (level <= 0) continue;
            bonus += registry.get(entry.getKey())
                    .flatMap(m -> m.tier(level))
                    .map(ModuleTier::manaRegenBonus)
                    .orElse(0);
        }
        return bonus;
    }
}
