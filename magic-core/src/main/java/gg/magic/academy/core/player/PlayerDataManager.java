package gg.magic.academy.core.player;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final Map<UUID, MagicPlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Load async, apply sync
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            MagicPlayerData data = db.loadPlayer(uuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> cache.put(uuid, data));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        MagicPlayerData data = cache.remove(uuid);
        if (data != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> db.savePlayer(data));
        }
    }

    /** Returns the cached data for an online player. Null if not loaded yet. */
    public MagicPlayerData get(Player player) {
        return cache.get(player.getUniqueId());
    }

    public MagicPlayerData get(UUID uuid) {
        return cache.get(uuid);
    }

    /** Save all online players — call on shutdown. */
    public void saveAll() {
        for (MagicPlayerData data : cache.values()) {
            db.savePlayer(data);
        }
    }

    /** Schedule periodic auto-save every 5 minutes. */
    public void startAutoSave() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (MagicPlayerData data : cache.values()) {
                db.savePlayer(data);
            }
        }, 6000L, 6000L); // 5 min in ticks
    }
}
