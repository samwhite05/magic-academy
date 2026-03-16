package gg.magic.academy.spells.command;

import gg.magic.academy.spells.MagicSpells;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpellbookCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (MagicSpells.get() == null) {
            player.sendMessage(Component.text("Spell system not available.")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }
        MagicSpells.get().getLoadoutMenu().open(player);
        return true;
    }
}
