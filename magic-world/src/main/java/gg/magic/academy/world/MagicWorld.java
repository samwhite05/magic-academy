package gg.magic.academy.world;

import gg.magic.academy.world.boss.WorldBossScheduler;
import gg.magic.academy.world.event.ManaStormController;
import gg.magic.academy.world.zone.ZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicWorld extends JavaPlugin {

    private static MagicWorld instance;
    private ZoneManager zoneManager;
    private ManaStormController manaStormController;
    private WorldBossScheduler worldBossScheduler;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        zoneManager = new ZoneManager(this);
        manaStormController = new ManaStormController(this);
        worldBossScheduler = new WorldBossScheduler(this);

        getServer().getPluginManager().registerEvents(zoneManager, this);

        manaStormController.schedule();
        worldBossScheduler.schedule();

        getLogger().info("MagicWorld enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagicWorld disabled.");
    }

    public static MagicWorld get() { return instance; }
    public static MagicWorld getInstance() {
        return (MagicWorld) Bukkit.getPluginManager().getPlugin("MagicWorld");
    }
    public ZoneManager getZoneManager() { return zoneManager; }
    public ManaStormController getManaStormController() { return manaStormController; }
    public WorldBossScheduler getWorldBossScheduler() { return worldBossScheduler; }
}
