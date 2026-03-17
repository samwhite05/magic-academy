package gg.magic.academy;

import gg.magic.academy.api.MagicCoreAPI;
import gg.magic.academy.contract.ContractBoardMenu;
import gg.magic.academy.contract.ContractCommand;
import gg.magic.academy.contract.ContractManager;
import gg.magic.academy.contract.ContractRegistry;
import gg.magic.academy.hotbar.AcademyHotbarListener;
import gg.magic.academy.npcs.npc.NpcRankTrialEvent;
import gg.magic.academy.rank.RankManager;
import gg.magic.academy.trial.TrialManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicAcademyPlugin extends JavaPlugin implements Listener {

    private static MagicAcademyPlugin instance;
    private RankManager rankManager;
    private TrialManager trialManager;
    private ContractRegistry contractRegistry;
    private ContractManager contractManager;
    private ContractBoardMenu contractBoardMenu;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        rankManager = new RankManager(this);
        trialManager = new TrialManager(this, rankManager);

        contractRegistry = new ContractRegistry(this);
        contractRegistry.loadAll();

        contractManager = new ContractManager(this, contractRegistry);
        contractBoardMenu = new ContractBoardMenu(contractManager);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(contractManager, this);
        getServer().getPluginManager().registerEvents(
                new AcademyHotbarListener(contractBoardMenu), this);

        if (getCommand("contracts") != null)
            getCommand("contracts").setExecutor(new ContractCommand(contractBoardMenu));

        getLogger().info("MagicAcademy enabled — " + contractRegistry.size() + " contracts loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagicAcademy disabled.");
    }

    @EventHandler
    public void onTrialNpc(NpcRankTrialEvent event) {
        trialManager.startTrial(event.getPlayer(), event.getTrialId());
    }

    @EventHandler
    public void onTrialBossDeath(EntityDeathEvent event) {
        trialManager.handleBossDeath(event.getEntity().getUniqueId());
    }

    public static MagicAcademyPlugin get() { return instance; }
    public static MagicAcademyPlugin getInstance() {
        return (MagicAcademyPlugin) Bukkit.getPluginManager().getPlugin("MagicAcademy");
    }
    public static MagicCoreAPI getCoreAPI() {
        return (MagicCoreAPI) Bukkit.getPluginManager().getPlugin("MagicCore");
    }
    public RankManager getRankManager() { return rankManager; }
    public TrialManager getTrialManager() { return trialManager; }
    public ContractManager getContractManager() { return contractManager; }
}
