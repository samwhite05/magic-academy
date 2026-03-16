package gg.magic.academy.npcs.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired when a player interacts with a rank trial NPC. */
public class NpcRankTrialEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String trialId;

    public NpcRankTrialEvent(Player player, String trialId) {
        super(player);
        this.trialId = trialId;
    }

    public String getTrialId() { return trialId; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
