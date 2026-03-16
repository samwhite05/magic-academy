package gg.magic.academy.npcs.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired when a player interacts with a dungeon portal NPC. */
public class NpcDungeonPortalEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String dungeonId;

    public NpcDungeonPortalEvent(Player player, String dungeonId) {
        super(player);
        this.dungeonId = dungeonId;
    }

    public String getDungeonId() { return dungeonId; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
