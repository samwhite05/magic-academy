package gg.magic.academy.api.event;

import gg.magic.academy.api.spell.SpellTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player crafts a rune combination that has never been discovered before.
 * The discoverer's name is recorded and announced server-wide.
 */
public class RuneDiscoveryEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpellTemplate discoveredSpell;
    private final String elementRuneId;
    private final String shapeRuneId;
    private final String effectRuneId;

    public RuneDiscoveryEvent(Player player, SpellTemplate discoveredSpell,
                               String elementRuneId, String shapeRuneId, String effectRuneId) {
        super(player);
        this.discoveredSpell = discoveredSpell;
        this.elementRuneId = elementRuneId;
        this.shapeRuneId = shapeRuneId;
        this.effectRuneId = effectRuneId;
    }

    public SpellTemplate getDiscoveredSpell() { return discoveredSpell; }
    public String getElementRuneId() { return elementRuneId; }
    public String getShapeRuneId() { return shapeRuneId; }
    public String getEffectRuneId() { return effectRuneId; }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
