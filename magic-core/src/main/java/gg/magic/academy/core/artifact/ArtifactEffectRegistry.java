package gg.magic.academy.core.artifact;

import gg.magic.academy.api.artifact.ArtifactEffectHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of named artifact effect types.
 * Built-in effects are registered on startup; content packs can add more.
 *
 * effectId -> handler
 */
public class ArtifactEffectRegistry {

    private final Map<String, ArtifactEffectHandler> effects = new HashMap<>();

    public ArtifactEffectRegistry() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        register("minor_mana_boost",   effect(50,  0.0,  0));
        register("major_mana_boost",   effect(150, 0.0,  0));
        register("minor_spell_power",  effect(0,   0.10, 0));
        register("major_spell_power",  effect(0,   0.25, 0));
        register("minor_mana_regen",   effect(0,   0.0,  2));
        register("major_mana_regen",   effect(0,   0.0,  5));
        register("balanced",           effect(30,  0.05, 1));
    }

    public void register(String effectId, ArtifactEffectHandler handler) {
        effects.put(effectId, handler);
    }

    public Optional<ArtifactEffectHandler> get(String effectId) {
        return Optional.ofNullable(effects.get(effectId));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static ArtifactEffectHandler effect(int mana, double dmg, int regen) {
        return new ArtifactEffectHandler() {
            @Override public int getManaBonus()     { return mana; }
            @Override public double getDamageBonus(){ return dmg;  }
            @Override public int getManaRegenBonus(){ return regen;}
        };
    }
}
