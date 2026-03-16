package gg.magic.academy.dungeons.instance;

import gg.magic.academy.api.AcademyRank;
import gg.magic.academy.api.event.DungeonCompleteEvent;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.dungeons.template.DungeonTemplate;
import gg.magic.academy.dungeons.template.DungeonTemplateLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the lifecycle of dungeon instances.
 *
 * World strategy: copy a template world folder into a uniquely-named folder per instance.
 * Uses Bukkit world management (Multiverse-Core or vanilla world loader) to load/unload.
 * If SlimeWorldManager is installed, it's used instead for faster async loading.
 */
public class DungeonInstanceManager implements Listener {

    private final JavaPlugin plugin;
    private final DungeonTemplateLoader templates;
    private gg.magic.academy.dungeons.party.PartyManager partyManager;

    /** instanceId -> DungeonInstance */
    private final Map<UUID, DungeonInstance> activeInstances = new HashMap<>();

    /** playerUUID -> instanceId (to look up which dungeon a player is in) */
    private final Map<UUID, UUID> playerToInstance = new HashMap<>();

    /** bossEntityUuid -> handler */
    private final Map<UUID, BossHandle> bossHandlers = new HashMap<>();

    public DungeonInstanceManager(JavaPlugin plugin, DungeonTemplateLoader templates) {
        this.plugin = plugin;
        this.templates = templates;
    }

    public void enterDungeon(Player player, String dungeonId) {
        // Check if player is already in a dungeon
        if (playerToInstance.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("✦ You are already in a dungeon!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Optional<DungeonTemplate> templateOpt = templates.get(dungeonId);
        if (templateOpt.isEmpty()) {
            player.sendMessage(Component.text("✦ Dungeon not found: " + dungeonId)
                    .color(TextColor.color(0xFF5555)));
            return;
        }
        DungeonTemplate template = templateOpt.get();

        // Rank gate check
        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data != null) {
            AcademyRank requiredRank = AcademyRank.valueOf(template.requiredRank());
            if (data.getRank().level() < requiredRank.level()) {
                player.sendMessage(Component.text("✦ You must be " + requiredRank.name() + " to enter this dungeon.")
                        .color(TextColor.color(0xFF5555)));
                return;
            }
        }

        player.sendMessage(Component.text("✦ Preparing dungeon instance, please wait...")
                .color(TextColor.color(0xFFAA00)));

        // Gather party members
        List<Player> partyPlayers;
        if (partyManager != null) {
            partyPlayers = partyManager.getPartyMembers(player.getUniqueId()).stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .toList();
        } else {
            partyPlayers = List.of(player);
        }
        DungeonInstance instance = new DungeonInstance(template, partyPlayers);
        activeInstances.put(instance.getInstanceId(), instance);
        for (Player p : partyPlayers) playerToInstance.put(p.getUniqueId(), instance.getInstanceId());

        // Load world async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String worldName = "dungeon_" + instance.getInstanceId().toString().substring(0, 8);
                copyTemplateWorld(dungeonId, worldName);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    World world = plugin.getServer().createWorld(
                            WorldCreator.name(worldName)
                                    .type(WorldType.FLAT)
                                    .generateStructures(false)
                    );
                    if (world == null) {
                        player.sendMessage(Component.text("✦ Failed to load dungeon. Try again.")
                                .color(TextColor.color(0xFF5555)));
                        cleanup(instance);
                        return;
                    }
                    instance.setWorld(world);
                    instance.setPhase(DungeonInstance.Phase.IN_PROGRESS);

                    // Teleport all party members to dungeon spawn
                    Location spawn = world.getSpawnLocation();
                    for (Player p : partyPlayers) {
                        p.teleport(spawn);
                        p.sendMessage(Component.text("✦ Welcome to " + template.name() + "!")
                                .color(TextColor.color(0x55FF55)));
                    }

                    startRoom(instance, player);
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to start dungeon instance", e);
                plugin.getServer().getScheduler().runTask(plugin, () -> cleanup(instance));
            }
        });
    }

    private void copyTemplateWorld(String dungeonId, String targetName) {
        File templateDir = new File(plugin.getDataFolder().getParentFile().getParentFile(),
                "plugins/MagicAcademy/dungeon_worlds/" + dungeonId);
        File targetDir = new File(plugin.getServer().getWorldContainer(), targetName);
        if (templateDir.exists()) {
            copyDir(templateDir, targetDir);
            // Remove session lock so Bukkit can load it
            new File(targetDir, "session.lock").delete();
        }
        // If no template world, we generate a blank one (acceptable for dev)
    }

    private void copyDir(File src, File dest) {
        dest.mkdirs();
        for (File file : Objects.requireNonNull(src.listFiles())) {
            File destFile = new File(dest, file.getName());
            if (file.isDirectory()) {
                copyDir(file, destFile);
            } else {
                try {
                    java.nio.file.Files.copy(file.toPath(), destFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to copy file: " + file.getName(), e);
                }
            }
        }
    }

    public void startRoom(DungeonInstance instance, Player player) {
        var room = instance.getCurrentRoom();
        if (room == null) {
            completeDungeon(instance);
            return;
        }
        RoomController controller = new RoomController(plugin, instance, room, this, () -> {
            // Room complete callback
            instance.clearActiveBoss();
            boolean hasMore = instance.advanceRoom();
            if (hasMore) {
                startRoom(instance, player);
            } else {
                completeDungeon(instance);
            }
        });
        controller.start(player);
    }

    private void completeDungeon(DungeonInstance instance) {
        instance.setPhase(DungeonInstance.Phase.COMPLETE);

        List<Player> onlinePlayers = instance.getParty().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();

        // Record clears and fire events
        for (Player p : onlinePlayers) {
            MagicCore.get().getDatabaseManager().recordDungeonClear(
                    p.getUniqueId(), instance.getTemplate().id(), instance.getDurationMs());
            plugin.getServer().getPluginManager().callEvent(
                    new DungeonCompleteEvent(p, instance.getTemplate().id(), onlinePlayers, instance.getDurationMs()));
            p.sendMessage(Component.text("✦ Dungeon complete! Returning to academy in 10 seconds...")
                    .color(TextColor.color(0xFFAA00)));
        }

        // Delayed cleanup and return
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location hub = plugin.getServer().getWorld("world") != null
                    ? plugin.getServer().getWorld("world").getSpawnLocation()
                    : new Location(Bukkit.getWorlds().get(0), 0, 64, 0);
            for (Player p : onlinePlayers) {
                p.teleport(hub);
            }
            cleanup(instance);
        }, 200L); // 10 seconds
    }

    private void cleanup(DungeonInstance instance) {
        for (UUID uuid : instance.getParty()) playerToInstance.remove(uuid);
        activeInstances.remove(instance.getInstanceId());
        clearBossesForInstance(instance.getInstanceId());

        if (instance.getWorld() != null) {
            World world = instance.getWorld();
            String worldName = world.getName();
            // Unload and delete
            plugin.getServer().unloadWorld(world, false);
            deleteWorldFolder(new File(plugin.getServer().getWorldContainer(), worldName));
        }
    }

    private void deleteWorldFolder(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) deleteWorldFolder(f);
            else f.delete();
        }
        dir.delete();
    }

    public void setPartyManager(gg.magic.academy.dungeons.party.PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    public void shutdownAll() {
        for (DungeonInstance instance : new ArrayList<>(activeInstances.values())) {
            cleanup(instance);
        }
    }

    public Optional<DungeonInstance> getInstanceForPlayer(UUID playerUuid) {
        UUID instanceId = playerToInstance.get(playerUuid);
        return instanceId == null ? Optional.empty() : Optional.ofNullable(activeInstances.get(instanceId));
    }

    public void registerBoss(UUID instanceId, UUID bossUuid, Runnable onComplete) {
        if (bossUuid == null) return;
        bossHandlers.put(bossUuid, new BossHandle(instanceId, onComplete));
    }

    public void handleBossDeath(UUID bossUuid) {
        BossHandle handle = bossHandlers.remove(bossUuid);
        if (handle != null && handle.onComplete() != null) {
            handle.onComplete().run();
        }
    }

    private void clearBossesForInstance(UUID instanceId) {
        bossHandlers.entrySet().removeIf(e -> e.getValue().instanceId().equals(instanceId));
    }

    private record BossHandle(UUID instanceId, Runnable onComplete) {}
}
