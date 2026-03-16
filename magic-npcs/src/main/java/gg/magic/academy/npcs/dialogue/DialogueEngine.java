package gg.magic.academy.npcs.dialogue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads and drives branching dialogue trees from plugins/MagicAcademy/dialogues/*.yml
 *
 * YAML format:
 *   headmaster_intro:
 *     start: welcome
 *     nodes:
 *       welcome:
 *         text: "Welcome to the Academy, young mage. Are you ready to begin your training?"
 *         options:
 *           - label: "Yes, I am ready."
 *             next: ready
 *           - label: "Tell me more about the Academy."
 *             next: about
 *       ready:
 *         text: "Excellent. Begin by visiting the Spell Laboratory."
 *         action: close
 *       about:
 *         text: "The Magic Academy has stood for a thousand years..."
 *         options:
 *           - label: "I am ready now."
 *             next: ready
 */
public class DialogueEngine {

    private final JavaPlugin plugin;

    /** dialogueId -> nodeId -> node */
    private final Map<String, Map<String, DialogueNode>> dialogues = new HashMap<>();

    /** dialogueId -> start node id */
    private final Map<String, String> startNodes = new HashMap<>();

    /** Tracks which node each player is currently on: playerUUID -> (dialogueId, nodeId) */
    private final Map<UUID, String[]> activeDialogue = new HashMap<>();

    public DialogueEngine(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        dialogues.clear();
        startNodes.clear();
        File dir = new File(plugin.getDataFolder().getParentFile().getParentFile(), "plugins/MagicAcademy/dialogues");
        if (!dir.exists()) { dir.mkdirs(); return; }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            for (String dialogueId : cfg.getKeys(false)) {
                try {
                    loadDialogue(dialogueId, cfg.getConfigurationSection(dialogueId));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load dialogue: " + dialogueId, e);
                }
            }
        }
    }

    private void loadDialogue(String id, ConfigurationSection s) {
        if (s == null) return;
        String start = s.getString("start");
        startNodes.put(id, start);

        Map<String, DialogueNode> nodes = new HashMap<>();
        ConfigurationSection nodesSection = s.getConfigurationSection("nodes");
        if (nodesSection == null) return;

        for (String nodeId : nodesSection.getKeys(false)) {
            ConfigurationSection ns = nodesSection.getConfigurationSection(nodeId);
            if (ns == null) continue;
            String text = ns.getString("text", "");
            String action = ns.getString("action", "");
            List<DialogueNode.DialogueOption> options = new ArrayList<>();
            for (Map<?, ?> opt : ns.getMapList("options")) {
                options.add(new DialogueNode.DialogueOption(
                        (String) opt.get("label"),
                        (String) opt.get("next")
                ));
            }
            nodes.put(nodeId, new DialogueNode(nodeId, text, options, action));
        }
        dialogues.put(id, nodes);
    }

    public void startDialogue(Player player, String dialogueId) {
        String startNode = startNodes.get(dialogueId);
        if (startNode == null) {
            plugin.getLogger().warning("Dialogue not found: " + dialogueId);
            return;
        }
        activeDialogue.put(player.getUniqueId(), new String[]{dialogueId, startNode});
        showNode(player, dialogueId, startNode);
    }

    private void showNode(Player player, String dialogueId, String nodeId) {
        Map<String, DialogueNode> nodes = dialogues.get(dialogueId);
        if (nodes == null) return;
        DialogueNode node = nodes.get(nodeId);
        if (node == null) return;

        // Handle action
        if (!node.action().isEmpty()) {
            handleAction(player, node.action());
        }

        // Display NPC text
        player.sendMessage(Component.text("")); // spacer
        player.sendMessage(Component.text("§8§m──────────────────────────────"));
        player.sendMessage(Component.text("§6✦ §e" + node.text()));
        player.sendMessage(Component.text("§8§m──────────────────────────────"));

        // Display options as clickable text
        for (int i = 0; i < node.options().size(); i++) {
            DialogueNode.DialogueOption opt = node.options().get(i);
            String cmd = "/magic_dialogue " + dialogueId + " " + opt.nextNodeId();
            player.sendMessage(
                    Component.text("  [" + (i + 1) + "] " + opt.label())
                            .color(TextColor.color(0x00FFAA))
                            .decoration(TextDecoration.ITALIC, false)
                            .clickEvent(ClickEvent.runCommand(cmd))
            );
        }

        if (node.options().isEmpty() && node.action().isEmpty()) {
            activeDialogue.remove(player.getUniqueId());
        }
    }

    /** Called by a command handler when a player clicks a dialogue option. */
    public void advance(Player player, String dialogueId, String nodeId) {
        String[] active = activeDialogue.get(player.getUniqueId());
        if (active == null || !active[0].equals(dialogueId)) return;
        if ("close".equals(nodeId)) {
            activeDialogue.remove(player.getUniqueId());
            return;
        }
        activeDialogue.put(player.getUniqueId(), new String[]{dialogueId, nodeId});
        showNode(player, dialogueId, nodeId);
    }

    private void handleAction(Player player, String action) {
        if (action.startsWith("quest_dispatch:")) {
            String questId = action.substring("quest_dispatch:".length());
            plugin.getServer().getPluginManager().callEvent(
                    new DialogueQuestDispatchEvent(player, questId));
        }
        // Additional action handlers (menu_open, etc.) can be added here
    }
}
