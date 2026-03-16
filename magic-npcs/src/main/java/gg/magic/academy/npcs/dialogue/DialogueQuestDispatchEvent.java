package gg.magic.academy.npcs.dialogue;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class DialogueQuestDispatchEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String questId;

    public DialogueQuestDispatchEvent(Player player, String questId) {
        super(player);
        this.questId = questId;
    }

    public String getQuestId() { return questId; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
