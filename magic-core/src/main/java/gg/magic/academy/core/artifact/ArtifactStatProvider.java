package gg.magic.academy.core.artifact;

import gg.magic.academy.api.artifact.ArtifactEffectHandler;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.stat.StatModifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements StatModifier by summing the bonuses of all active artifacts.
 * magic-items calls mapArtifact() for each loaded artifact so we know effectId -> handler.
 */
public class ArtifactStatProvider implements StatModifier {

    /** artifactId -> handler (populated by ArtifactRegistry in magic-items) */
    private final Map<String, ArtifactEffectHandler> artifactHandlers = new HashMap<>();

    public void mapArtifact(String artifactId, ArtifactEffectHandler handler) {
        artifactHandlers.put(artifactId, handler);
    }

    @Override
    public int getMaxManaBonus(MagicPlayerData data) {
        int bonus = 0;
        for (String id : data.getActiveArtifacts()) {
            ArtifactEffectHandler h = artifactHandlers.get(id);
            if (h != null) bonus += h.getManaBonus();
        }
        return bonus;
    }

    @Override
    public double getSpellDamageBonus(MagicPlayerData data) {
        double bonus = 0.0;
        for (String id : data.getActiveArtifacts()) {
            ArtifactEffectHandler h = artifactHandlers.get(id);
            if (h != null) bonus += h.getDamageBonus();
        }
        return bonus;
    }

    @Override
    public int getManaRegenBonus(MagicPlayerData data) {
        int bonus = 0;
        for (String id : data.getActiveArtifacts()) {
            ArtifactEffectHandler h = artifactHandlers.get(id);
            if (h != null) bonus += h.getManaRegenBonus();
        }
        return bonus;
    }
}
