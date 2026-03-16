package gg.magic.academy.contract;

import gg.magic.academy.api.Rarity;

import java.util.List;
import java.util.Map;

/**
 * Static definition of a contract loaded from YAML.
 *
 * @param id          Unique contract id
 * @param name        Display name
 * @param description Flavour text
 * @param rarity      Visual rarity
 * @param objectives  Ordered list of objectives
 * @param rewardItems Map of itemId -> amount granted on completion
 * @param rewardMana  Mana restored on completion (0 = none)
 */
public record ContractTemplate(
        String id,
        String name,
        String description,
        Rarity rarity,
        List<ContractObjective> objectives,
        Map<String, Integer> rewardItems,
        int rewardMana
) {}
