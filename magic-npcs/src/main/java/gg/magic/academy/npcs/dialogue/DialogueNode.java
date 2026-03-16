package gg.magic.academy.npcs.dialogue;

import java.util.List;

/**
 * A single step in a dialogue tree.
 */
public record DialogueNode(
        String id,
        String text,
        List<DialogueOption> options,
        String action    // optional: "quest_dispatch:quest_id", "close", "menu:menu_id", etc.
) {
    public record DialogueOption(
            String label,
            String nextNodeId   // which node to show next, or "close" to end
    ) {}
}
