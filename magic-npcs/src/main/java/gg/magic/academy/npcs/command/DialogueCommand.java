package gg.magic.academy.npcs.command;

import gg.magic.academy.npcs.dialogue.DialogueEngine;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DialogueCommand implements CommandExecutor {

    private final DialogueEngine dialogueEngine;

    public DialogueCommand(DialogueEngine dialogueEngine) {
        this.dialogueEngine = dialogueEngine;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length < 2) return true;

        String dialogueId = args[0];
        String nodeId = args[1];
        dialogueEngine.advance(player, dialogueId, nodeId);
        return true;
    }
}
