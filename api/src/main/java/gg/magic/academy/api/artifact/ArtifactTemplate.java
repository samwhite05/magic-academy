package gg.magic.academy.api.artifact;

import gg.magic.academy.api.Rarity;

/**
 * Static definition of an artifact/relic loaded from YAML.
 */
public record ArtifactTemplate(
        String id,
        String name,
        ArtifactSource source,
        String effectId,      // references an effect handler registered in magic-core
        Rarity rarity,
        int customModelData
) {}
