package gg.magic.academy.hideouts;

import gg.magic.academy.core.MagicCore;
import gg.magic.academy.hideouts.command.HideoutCommand;
import gg.magic.academy.hideouts.hotbar.HideoutHotbarListener;
import gg.magic.academy.hideouts.manager.HideoutManager;
import gg.magic.academy.hideouts.menu.ArtifactDisplayMenu;
import gg.magic.academy.hideouts.menu.ModuleUpgradeMenu;
import gg.magic.academy.hideouts.module.HideoutBuffProvider;
import gg.magic.academy.hideouts.module.ModuleRegistry;
import gg.magic.academy.items.MagicItems;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicHideouts extends JavaPlugin {

    private static MagicHideouts instance;
    private ModuleRegistry moduleRegistry;
    private HideoutManager hideoutManager;
    private HideoutBuffProvider buffProvider;
    private ModuleUpgradeMenu upgradeMenu;
    private ArtifactDisplayMenu artifactDisplayMenu;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        moduleRegistry = new ModuleRegistry(this);
        moduleRegistry.loadAll();

        hideoutManager = new HideoutManager(this, moduleRegistry);
        buffProvider = new HideoutBuffProvider(moduleRegistry);
        upgradeMenu = new ModuleUpgradeMenu(moduleRegistry);

        MagicItems itemsPlugin = (MagicItems) Bukkit.getPluginManager().getPlugin("MagicItems");
        artifactDisplayMenu = new ArtifactDisplayMenu(itemsPlugin.getArtifactRegistry());

        MagicCore corePlugin = (MagicCore) Bukkit.getPluginManager().getPlugin("MagicCore");
        corePlugin.getStatEngine().registerModifier("hideout_buffs", buffProvider);

        getServer().getPluginManager().registerEvents(hideoutManager, this);
        getServer().getPluginManager().registerEvents(new HideoutHotbarListener(hideoutManager), this);

        if (getCommand("hideout") != null) {
            getCommand("hideout").setExecutor(new HideoutCommand(hideoutManager, upgradeMenu, artifactDisplayMenu));
        }

        getLogger().info("MagicHideouts enabled — " + moduleRegistry.size() + " modules loaded.");
    }

    @Override
    public void onDisable() {
        MagicCore corePlugin = (MagicCore) Bukkit.getPluginManager().getPlugin("MagicCore");
        if (corePlugin != null) {
            corePlugin.getStatEngine().unregisterModifier("hideout_buffs");
        }
        getLogger().info("MagicHideouts disabled.");
    }

    public static MagicHideouts get() { return instance; }
    public static MagicHideouts getInstance() {
        return (MagicHideouts) Bukkit.getPluginManager().getPlugin("MagicHideouts");
    }
    public ModuleRegistry getModuleRegistry() { return moduleRegistry; }
    public HideoutManager getHideoutManager() { return hideoutManager; }
    public ArtifactDisplayMenu getArtifactDisplayMenu() { return artifactDisplayMenu; }
}
