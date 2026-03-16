package gg.magic.academy.dungeons.instance;

import gg.magic.academy.dungeons.template.DungeonTemplate;
import gg.magic.academy.dungeons.template.RoomConfig;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * A running dungeon instance for a player or party.
 */
public class DungeonInstance {

    public enum Phase {
        LOADING,
        IN_PROGRESS,
        COMPLETE,
        FAILED
    }

    private final UUID instanceId;
    private final DungeonTemplate template;
    private final List<UUID> party;       // player UUIDs
    private World world;
    private Phase phase = Phase.LOADING;
    private int currentRoomIndex = 0;
    private final long startTime = System.currentTimeMillis();
    private UUID activeBossUuid;

    public DungeonInstance(DungeonTemplate template, List<Player> party) {
        this.instanceId = UUID.randomUUID();
        this.template = template;
        this.party = party.stream().map(Player::getUniqueId).toList();
    }

    public UUID getInstanceId() { return instanceId; }
    public DungeonTemplate getTemplate() { return template; }
    public List<UUID> getParty() { return party; }
    public World getWorld() { return world; }
    public void setWorld(World world) { this.world = world; }
    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }
    public long getStartTime() { return startTime; }
    public long getDurationMs() { return System.currentTimeMillis() - startTime; }

    public RoomConfig getCurrentRoom() {
        List<RoomConfig> rooms = template.rooms();
        if (currentRoomIndex >= rooms.size()) return null;
        return rooms.get(currentRoomIndex);
    }

    public boolean advanceRoom() {
        currentRoomIndex++;
        return currentRoomIndex < template.rooms().size();
    }

    public boolean isLastRoom() {
        return currentRoomIndex >= template.rooms().size() - 1;
    }

    public UUID getActiveBossUuid() { return activeBossUuid; }
    public void setActiveBossUuid(UUID activeBossUuid) { this.activeBossUuid = activeBossUuid; }
    public void clearActiveBoss() { this.activeBossUuid = null; }

    /** Scale factor for mob HP based on party size. */
    public double getHpScalar() {
        int size = party.size();
        if (size <= 1) return template.soloHpMult();
        return template.partyHpMult() + (size - 1) * template.perExtraPlayerAdd();
    }
}
