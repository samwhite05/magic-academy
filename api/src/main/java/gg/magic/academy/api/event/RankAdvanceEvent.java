package gg.magic.academy.api.event;

import gg.magic.academy.api.AcademyRank;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class RankAdvanceEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AcademyRank previousRank;
    private final AcademyRank newRank;

    public RankAdvanceEvent(Player player, AcademyRank previousRank, AcademyRank newRank) {
        super(player);
        this.previousRank = previousRank;
        this.newRank = newRank;
    }

    public AcademyRank getPreviousRank() { return previousRank; }
    public AcademyRank getNewRank() { return newRank; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
