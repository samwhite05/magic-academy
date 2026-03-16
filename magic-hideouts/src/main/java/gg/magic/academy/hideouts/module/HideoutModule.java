package gg.magic.academy.hideouts.module;

import java.util.List;
import java.util.Optional;

public record HideoutModule(
        String id,
        String name,
        String description,
        List<ModuleTier> tiers
) {
    public int maxTier() { return tiers.size(); }

    public Optional<ModuleTier> tier(int tierNumber) {
        return tiers.stream().filter(t -> t.tier() == tierNumber).findFirst();
    }
}
