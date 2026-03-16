package gg.magic.academy.npcs;

import gg.magic.academy.npcs.dialogue.DialogueEngine;
import gg.magic.academy.npcs.npc.NpcManager;
import gg.magic.academy.npcs.command.DialogueCommand;
import gg.magic.academy.npcs.command.NpcsCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MagicNpcs extends JavaPlugin {

    private static MagicNpcs instance;
    private NpcManager npcManager;
    private DialogueEngine dialogueEngine;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dialogueEngine = new DialogueEngine(this);
        dialogueEngine.loadAll();

        npcManager = new NpcManager(this, dialogueEngine);
        npcManager.loadAll();

        getServer().getPluginManager().registerEvents(npcManager, this);
        if (getCommand("magic_dialogue") != null) {
            getCommand("magic_dialogue").setExecutor(new DialogueCommand(dialogueEngine));
        }
        if (getCommand("npcs") != null) {
            getCommand("npcs").setExecutor(new NpcsCommand());
        }

        // Spawn NPCs once worlds are loaded
        getServer().getScheduler().runTaskLater(this, npcManager::spawnAll, 20L);

        getLogger().info("MagicNpcs enabled — " + npcManager.getNpcCount() + " NPCs configured.");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) npcManager.despawnAll();
        getLogger().info("MagicNpcs disabled.");
    }

    public static MagicNpcs get() { return instance; }
    public NpcManager getNpcManager() { return npcManager; }
    public DialogueEngine getDialogueEngine() { return dialogueEngine; }
}
