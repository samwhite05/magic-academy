package gg.magic.academy.contract;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ContractCommand implements CommandExecutor {

    private final ContractBoardMenu menu;

    public ContractCommand(ContractBoardMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        menu.open(player, 0);
        return true;
    }
}
