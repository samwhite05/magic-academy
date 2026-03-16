package gg.magic.academy.dungeons.template;

import java.util.Map;

/**
 * Configuration for a single room in a dungeon template.
 */
public record RoomConfig(
        RoomType type,
        String config,          // puzzle config id (PUZZLE type)
        String mythicmobsPack,  // mob wave pack id (MOB_WAVE type)
        int waveCount,          // number of waves (MOB_WAVE type)
        String mythicmobsMob,   // mob id (MINIBOSS/BOSS type)
        String lootTableId      // loot table (TREASURE type)
) {
    public enum RoomType {
        PUZZLE,
        MOB_WAVE,
        MINIBOSS,
        BOSS,
        TREASURE
    }
}
