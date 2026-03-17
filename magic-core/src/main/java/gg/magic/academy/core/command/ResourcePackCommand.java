package gg.magic.academy.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * /packresend [player]
 *   - No args: resends the resource pack to the sender.
 *   - With player arg (requires magic.admin): resends to that player.
 *   - "all" arg (requires magic.admin): resends to every online player.
 */
public class ResourcePackCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public ResourcePackCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String url  = plugin.getServer().getResourcePack();
        String hash = plugin.getServer().getResourcePackHash();

        if (url == null || url.isEmpty()) {
            sender.sendMessage(Component.text("✦ No resource pack is configured on this server.")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        if (args.length == 0) {
            // Send to self
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("✦ Specify a player or 'all'.")
                        .color(TextColor.color(0xFF5555)));
                return true;
            }
            sendPack(player, url, hash);
            sender.sendMessage(Component.text("✦ Resource pack resent.")
                    .color(TextColor.color(0x55FF55)));
            return true;
        }

        if (!sender.hasPermission("magic.admin")) {
            sender.sendMessage(Component.text("✦ No permission.")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            for (Player p : Bukkit.getOnlinePlayers()) sendPack(p, url, hash);
            sender.sendMessage(Component.text("✦ Resource pack resent to " + Bukkit.getOnlinePlayers().size() + " player(s).")
                    .color(TextColor.color(0x55FF55)));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("✦ Player not found: " + args[0])
                    .color(TextColor.color(0xFF5555)));
            return true;
        }
        sendPack(target, url, hash);
        sender.sendMessage(Component.text("✦ Resource pack resent to " + target.getName() + ".")
                .color(TextColor.color(0x55FF55)));
        return true;
    }

    @SuppressWarnings("deprecation")
    private void sendPack(Player player, String url, String hash) {
        player.setResourcePack(url, hash);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("magic.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }
}
