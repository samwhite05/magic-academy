package gg.magic.academy.dungeons.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyCommand implements CommandExecutor {

    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            partyManager.showInfo(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "invite" -> {
                if (args.length < 2) { player.sendMessage("Usage: /party invite <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { player.sendMessage("Player not found: " + args[1]); return true; }
                partyManager.invite(player, target);
            }
            case "accept"  -> partyManager.acceptInvite(player);
            case "decline" -> partyManager.declineInvite(player);
            case "leave"   -> partyManager.leave(player);
            case "disband" -> partyManager.disband(player);
            case "kick" -> {
                if (args.length < 2) { player.sendMessage("Usage: /party kick <player>"); return true; }
                partyManager.kick(player, args[1]);
            }
            case "info"    -> partyManager.showInfo(player);
            default -> player.sendMessage("Usage: /party [invite|accept|decline|leave|kick|disband|info]");
        }
        return true;
    }
}
