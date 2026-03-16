package gg.magic.academy.npcs.npc;

import gg.magic.academy.npcs.dialogue.DialogueEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Spawns and manages lightweight NPCs using Villager entities.
 * NPCs are invisible, have AI disabled, and are identified by PDC.
 */
public class NpcManager implements Listener {

    private static final NamespacedKey NPC_ID_KEY = new NamespacedKey("magic", "npc_id");

    private final JavaPlugin plugin;
    private final DialogueEngine dialogueEngine;

    private final Map<String, NpcDefinition> definitions = new HashMap<>();
    private final Map<UUID, String> spawnedEntities = new HashMap<>(); // entity UUID -> npc id

    public NpcManager(JavaPlugin plugin, DialogueEngine dialogueEngine) {
        this.plugin = plugin;
        this.dialogueEngine = dialogueEngine;
    }

    public void loadAll() {
        definitions.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/npcs");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String key : cfg.getKeys(false)) {
                try {
                    ConfigurationSection s = cfg.getConfigurationSection(key);
                    if (s == null) continue;
                    ConfigurationSection loc = s.getConfigurationSection("location");
                    NpcDefinition def = new NpcDefinition(
                            s.getString("id", key),
                            s.getString("name", key),
                            s.getString("world", "world"),
                            loc != null ? loc.getDouble("x") : 0,
                            loc != null ? loc.getDouble("y") : 64,
                            loc != null ? loc.getDouble("z") : 0,
                            loc != null ? (float) loc.getDouble("yaw") : 0,
                            NpcDefinition.InteractionType.valueOf(
                                    s.getString("interaction.type", "DIALOGUE").toUpperCase()),
                            s.getString("interaction.target", "")
                    );
                    definitions.put(def.id(), def);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load NPC: " + key, e);
                }
            }
        }
    }

    public void spawnAll() {
        for (NpcDefinition def : definitions.values()) {
            spawn(def);
        }
    }

    private void spawn(NpcDefinition def) {
        World world = Bukkit.getWorld(def.worldName());
        if (world == null) {
            plugin.getLogger().warning("World not found for NPC " + def.id() + ": " + def.worldName());
            return;
        }
        Location loc = new Location(world, def.x(), def.y(), def.z(), def.yaw(), 0);
        Villager villager = world.spawn(loc, Villager.class, v -> {
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setVillagerType(Villager.Type.PLAINS);
            v.setProfession(Villager.Profession.NONE);
            v.setPersistent(true);
            v.customName(Component.text(def.name()).decoration(TextDecoration.ITALIC, false));
            v.setCustomNameVisible(true);
            v.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, def.id());
        });
        spawnedEntities.put(villager.getUniqueId(), def.id());
    }

    public void despawnAll() {
        for (UUID entityUUID : spawnedEntities.keySet()) {
            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity != null) entity.remove();
        }
        spawnedEntities.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        String npcId = entity.getPersistentDataContainer().get(NPC_ID_KEY, PersistentDataType.STRING);
        if (npcId == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        NpcDefinition def = definitions.get(npcId);
        if (def == null) return;

        switch (def.interactionType()) {
            case DIALOGUE -> dialogueEngine.startDialogue(player, def.interactionTarget());
            case DUNGEON_PORTAL -> plugin.getServer().getPluginManager()
                    .callEvent(new NpcDungeonPortalEvent(player, def.interactionTarget()));
            case RANK_TRIAL -> plugin.getServer().getPluginManager()
                    .callEvent(new NpcRankTrialEvent(player, def.interactionTarget()));
            case VENDOR_MENU -> {
                // Vendor menus handled in a separate class — for now send placeholder
                player.sendMessage(Component.text("[Vendor] Shop coming soon!"));
            }
        }
    }

    public int getNpcCount() { return definitions.size(); }
}
