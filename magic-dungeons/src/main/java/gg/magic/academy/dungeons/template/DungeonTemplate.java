package gg.magic.academy.dungeons.template;

import java.util.List;

public record DungeonTemplate(
        String id,
        String name,
        String theme,
        int difficultyBase,
        List<RoomConfig> rooms,
        double soloHpMult,
        double partyHpMult,
        double perExtraPlayerAdd,
        String requiredRank       // minimum AcademyRank name to enter
) {}
