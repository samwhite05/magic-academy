package gg.magic.academy.dungeons;

import gg.magic.academy.dungeons.instance.DungeonBossDeathListener;
import gg.magic.academy.dungeons.instance.DungeonInstanceManager;
import gg.magic.academy.dungeons.party.PartyCommand;
import gg.magic.academy.dungeons.party.PartyManager;
import gg.magic.academy.dungeons.template.DungeonTemplateLoader;
import gg.magic.academy.npcs.npc.NpcDungeonPortalEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicDungeons extends JavaPlugin implements Listener {

    private static MagicDungeons instance;
    private DungeonTemplateLoader templateLoader;
    private DungeonInstanceManager instanceManager;
    private PartyManager partyManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        templateLoader = new DungeonTemplateLoader(this);
        templateLoader.loadAll();

        partyManager = new PartyManager(this);
        instanceManager = new DungeonInstanceManager(this, templateLoader);
        instanceManager.setPartyManager(partyManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(instanceManager, this);
        getServer().getPluginManager().registerEvents(new DungeonBossDeathListener(instanceManager), this);

        if (getCommand("party") != null)
            getCommand("party").setExecutor(new PartyCommand(partyManager));

        getLogger().info("MagicDungeons enabled — " + templateLoader.size() + " dungeons loaded.");
    }

    @Override
    public void onDisable() {
        if (instanceManager != null) instanceManager.shutdownAll();
        getLogger().info("MagicDungeons disabled.");
    }

    @EventHandler
    public void onDungeonPortal(NpcDungeonPortalEvent event) {
        instanceManager.enterDungeon(event.getPlayer(), event.getDungeonId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        partyManager.removePlayer(event.getPlayer().getUniqueId());
    }

    public static MagicDungeons get() { return instance; }
    public static MagicDungeons getInstance() {
        return (MagicDungeons) Bukkit.getPluginManager().getPlugin("MagicDungeons");
    }
    public DungeonTemplateLoader getTemplateLoader() { return templateLoader; }
    public DungeonInstanceManager getInstanceManager() { return instanceManager; }
    public PartyManager getPartyManager() { return partyManager; }
}
