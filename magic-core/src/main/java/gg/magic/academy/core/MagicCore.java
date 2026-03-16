package gg.magic.academy.core;

import gg.magic.academy.core.artifact.ArtifactEffectRegistry;
import gg.magic.academy.core.artifact.ArtifactStatProvider;
import gg.magic.academy.core.database.DatabaseManager;
import gg.magic.academy.core.hotbar.LobbyHotbarManager;
import gg.magic.academy.core.mana.ManaSystem;
import gg.magic.academy.core.player.PlayerDataManager;
import gg.magic.academy.core.stat.StatEngine;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicCore extends JavaPlugin {

    private static MagicCore instance;

    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private ManaSystem manaSystem;
    private StatEngine statEngine;
    private ArtifactEffectRegistry artifactEffectRegistry;
    private ArtifactStatProvider artifactStatProvider;
    private LobbyHotbarManager lobbyHotbarManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        playerDataManager = new PlayerDataManager(this, databaseManager);
        statEngine = new StatEngine();

        artifactEffectRegistry = new ArtifactEffectRegistry();
        artifactStatProvider = new ArtifactStatProvider();
        statEngine.registerModifier("artifact_buffs", artifactStatProvider);

        manaSystem = new ManaSystem(this, playerDataManager);
        lobbyHotbarManager = new LobbyHotbarManager(this);

        getServer().getPluginManager().registerEvents(playerDataManager, this);
        getServer().getPluginManager().registerEvents(lobbyHotbarManager, this);
        manaSystem.startRegenTask();
        playerDataManager.startAutoSave();

        // Register PlaceholderAPI expansion if present
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new gg.magic.academy.core.placeholder.MagicPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("MagicCore enabled.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("MagicCore disabled.");
    }

    public static MagicCore get() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public ManaSystem getManaSystem() { return manaSystem; }
    public StatEngine getStatEngine() { return statEngine; }
    public ArtifactEffectRegistry getArtifactEffectRegistry() { return artifactEffectRegistry; }
    public ArtifactStatProvider getArtifactStatProvider() { return artifactStatProvider; }
    public LobbyHotbarManager getLobbyHotbarManager() { return lobbyHotbarManager; }
}
