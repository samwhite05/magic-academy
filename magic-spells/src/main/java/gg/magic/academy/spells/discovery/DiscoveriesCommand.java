package gg.magic.academy.spells.discovery;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscoveriesCommand implements CommandExecutor {

    private final DiscoveriesMenu menu;

    public DiscoveriesCommand(DiscoveriesMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        menu.open(player, 0);
        return true;
    }
}
