package gg.magic.academy.api.rune;

/**
 * Static definition of a rune loaded from YAML.
 */
public record RuneTemplate(
        String id,
        String name,
        RuneType type,
        int powerTier,
        int customModelData
) {}
