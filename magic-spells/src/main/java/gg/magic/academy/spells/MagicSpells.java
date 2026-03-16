package gg.magic.academy.spells;

import gg.magic.academy.spells.command.SpellUpgradeCommand;
import gg.magic.academy.spells.command.SpellbookCommand;
import gg.magic.academy.spells.crafting.RuneCraftingHandler;
import gg.magic.academy.spells.discovery.DiscoveriesCommand;
import gg.magic.academy.spells.discovery.DiscoveriesListener;
import gg.magic.academy.spells.discovery.DiscoveriesMenu;
import gg.magic.academy.spells.executor.SpellExecutor;
import gg.magic.academy.spells.hotbar.SpellHotbarListener;
import gg.magic.academy.spells.loadout.SpellLoadoutListener;
import gg.magic.academy.spells.loadout.SpellLoadoutMenu;
import gg.magic.academy.spells.registry.RuneRegistry;
import gg.magic.academy.spells.registry.SpellRegistry;
import gg.magic.academy.spells.upgrade.SpellUpgradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicSpells extends JavaPlugin {

    private static MagicSpells instance;

    private SpellRegistry spellRegistry;
    private RuneRegistry runeRegistry;
    private SpellExecutor spellExecutor;
    private RuneCraftingHandler runeCraftingHandler;
    private SpellUpgradeManager upgradeManager;
    private SpellLoadoutMenu loadoutMenu;
    private SpellLoadoutListener loadoutListener;
    private DiscoveriesMenu discoveriesMenu;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        spellRegistry = new SpellRegistry(this);
        spellRegistry.loadAll();

        runeRegistry = new RuneRegistry(this);
        runeRegistry.loadAll();

        spellExecutor = new SpellExecutor(this);
        upgradeManager = new SpellUpgradeManager(this);
        runeCraftingHandler = new RuneCraftingHandler(this, spellRegistry, runeRegistry, upgradeManager);
        loadoutMenu = new SpellLoadoutMenu(this, spellRegistry);
        loadoutListener = new SpellLoadoutListener(loadoutMenu);
        discoveriesMenu = new DiscoveriesMenu(this, spellRegistry);

        getServer().getPluginManager().registerEvents(runeCraftingHandler, this);
        getServer().getPluginManager().registerEvents(spellExecutor, this);
        getServer().getPluginManager().registerEvents(loadoutListener, this);
        getServer().getPluginManager().registerEvents(new DiscoveriesListener(discoveriesMenu), this);
        getServer().getPluginManager().registerEvents(
                new SpellHotbarListener(loadoutMenu, discoveriesMenu), this);

        if (getCommand("spellbook") != null)
            getCommand("spellbook").setExecutor(new SpellbookCommand());
        if (getCommand("spellupgrade") != null)
            getCommand("spellupgrade").setExecutor(new SpellUpgradeCommand());
        if (getCommand("discoveries") != null)
            getCommand("discoveries").setExecutor(new DiscoveriesCommand(discoveriesMenu));

        getLogger().info("MagicSpells enabled — " + spellRegistry.size() + " spells loaded.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagicSpells disabled.");
    }

    public static MagicSpells get() { return instance; }
    public SpellRegistry getSpellRegistry() { return spellRegistry; }
    public RuneRegistry getRuneRegistry() { return runeRegistry; }
    public SpellExecutor getSpellExecutor() { return spellExecutor; }
    public SpellLoadoutMenu getLoadoutMenu() { return loadoutMenu; }
    public SpellUpgradeManager getUpgradeManager() { return upgradeManager; }
    public DiscoveriesMenu getDiscoveriesMenu() { return discoveriesMenu; }
}
