package gg.magic.academy.rank;

import java.util.List;
import java.util.Map;

/**
 * Requirements to advance to a specific rank.
 */
public record RankGate(
        String targetRank,
        List<String> requiredDungeonClears,    // dungeonId -> must have at least 1 clear
        List<String> requiredSpells,           // must own these spell IDs
        int requiredDiscoveries,               // number of unique spell discoveries
        String trialId                         // trial that must be passed
) {}
