package gg.magic.academy.npcs.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/**
 * Static definition of an NPC loaded from YAML.
 */
public record NpcDefinition(
        String id,
        String name,            // display name, supports §-color codes
        String worldName,
        double x, double y, double z,
        float yaw,
        InteractionType interactionType,
        String interactionTarget   // dialogue_id or menu_id
) {
    public enum InteractionType {
        DIALOGUE,
        VENDOR_MENU,
        DUNGEON_PORTAL,
        RANK_TRIAL
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, 0);
    }
}
