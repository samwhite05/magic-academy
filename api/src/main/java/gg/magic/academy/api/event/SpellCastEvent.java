package gg.magic.academy.api.event;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class SpellCastEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final MagicPlayerData playerData;
    private final SpellTemplate spell;
    private final int tier;

    public SpellCastEvent(Player player, MagicPlayerData playerData, SpellTemplate spell, int tier) {
        super(player);
        this.playerData = playerData;
        this.spell = spell;
        this.tier = tier;
    }

    public MagicPlayerData getPlayerData() { return playerData; }
    public SpellTemplate getSpell() { return spell; }
    public int getTier() { return tier; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
