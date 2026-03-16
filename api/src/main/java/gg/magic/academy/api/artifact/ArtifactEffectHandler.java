package gg.magic.academy.api.artifact;

/**
 * Defines the stat bonuses granted by an artifact when it is active (displayed in hideout).
 * Implementations are registered in magic-core's ArtifactEffectRegistry by effectId.
 */
public interface ArtifactEffectHandler {
    /** Flat bonus to max mana. */
    int getManaBonus();
    /** Additive spell damage multiplier bonus (e.g. 0.10 = +10%). */
    double getDamageBonus();
    /** Flat bonus to mana regen per second. */
    int getManaRegenBonus();
}
