package gg.magic.academy.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DungeonCompleteEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String dungeonId;
    private final List<Player> party;
    private final long durationMs;

    public DungeonCompleteEvent(Player player, String dungeonId, List<Player> party, long durationMs) {
        super(player);
        this.dungeonId = dungeonId;
        this.party = List.copyOf(party);
        this.durationMs = durationMs;
    }

    public String getDungeonId() { return dungeonId; }
    public List<Player> getParty() { return party; }
    public long getDurationMs() { return durationMs; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
