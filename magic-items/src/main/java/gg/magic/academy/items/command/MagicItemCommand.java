package gg.magic.academy.items.command;

import gg.magic.academy.items.MagicItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class MagicItemCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /magicitem <id> [amount]")
                    .color(TextColor.color(0xAAAAAA)));
            return true;
        }

        MagicItems items = MagicItems.get();
        if (items == null) return true;

        String id = args[0].toLowerCase();
        if (id.equals("list")) {
            var ids = items.getItemRegistry().getIds().stream().sorted().toList();
            if (ids.isEmpty()) {
                player.sendMessage(Component.text("No items registered.")
                        .color(TextColor.color(0xAAAAAA)));
                return true;
            }
            player.sendMessage(Component.text("Magic items:")
                    .color(TextColor.color(0xAAAAAA)));
            int i = 0;
            StringBuilder line = new StringBuilder();
            for (String itemId : ids) {
                if (i > 0) line.append(", ");
                line.append(itemId);
                i++;
                if (i % 8 == 0) {
                    player.sendMessage(Component.text(line.toString())
                            .color(TextColor.color(0xCCCCCC)));
                    line.setLength(0);
                    i = 0;
                }
            }
            if (!line.isEmpty()) {
                player.sendMessage(Component.text(line.toString())
                        .color(TextColor.color(0xCCCCCC)));
            }
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }

        Optional<org.bukkit.inventory.ItemStack> stackOpt = items.getItemRegistry().get(id, amount);
        if (stackOpt.isEmpty()) {
            player.sendMessage(Component.text("Unknown item id: " + id)
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        player.getInventory().addItem(stackOpt.get());
        player.sendMessage(Component.text("Given " + amount + "x " + id)
                .color(TextColor.color(0x55FF55)));
        return true;
    }
}
