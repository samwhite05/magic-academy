package gg.magic.academy.items;

import gg.magic.academy.items.command.MagicItemCommand;
import gg.magic.academy.items.listener.ArtifactPickupListener;
import gg.magic.academy.items.loot.LootTableRegistry;
import gg.magic.academy.items.registry.ArtifactRegistry;
import gg.magic.academy.items.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicItems extends JavaPlugin {

    private static MagicItems instance;
    private ItemRegistry itemRegistry;
    private LootTableRegistry lootTableRegistry;
    private ArtifactRegistry artifactRegistry;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        itemRegistry = new ItemRegistry(this);
        itemRegistry.loadAll();

        artifactRegistry = new ArtifactRegistry(this);
        artifactRegistry.loadAll();

        lootTableRegistry = new LootTableRegistry(this);
        lootTableRegistry.loadAll();

        getServer().getPluginManager().registerEvents(new ArtifactPickupListener(this), this);

        if (getCommand("magicitem") != null) {
            getCommand("magicitem").setExecutor(new MagicItemCommand());
        }

        getLogger().info("MagicItems enabled — " + itemRegistry.size() + " items, "
                + artifactRegistry.size() + " artifacts registered.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MagicItems disabled.");
    }

    public static MagicItems get() { return instance; }
    public static MagicItems getInstance() {
        return (MagicItems) Bukkit.getPluginManager().getPlugin("MagicItems");
    }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public LootTableRegistry getLootTableRegistry() { return lootTableRegistry; }
    public ArtifactRegistry getArtifactRegistry() { return artifactRegistry; }
}
