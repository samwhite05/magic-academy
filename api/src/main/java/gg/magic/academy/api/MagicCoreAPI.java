package gg.magic.academy.api;

import gg.magic.academy.api.artifact.ArtifactEffectHandler;
import java.util.Collection;

public interface MagicCoreAPI {
    ArtifactEffectHandler getArtifactEffectHandler(String effectId);
    Collection<String> getRegisteredEffectIds();
    void registerArtifactEffect(String artifactId, ArtifactEffectHandler handler);

    Object getDatabaseManager();
    Object getPlayerDataManager();
    Object getStatEngine();
}
