package gg.magic.academy.npcs.command;

import gg.magic.academy.npcs.MagicNpcs;
import gg.magic.academy.npcs.npc.NpcManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NpcsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("magic.npcs.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        MagicNpcs plugin = MagicNpcs.get();
        if (plugin == null) return true;
        NpcManager manager = plugin.getNpcManager();
        if (manager == null) return true;

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /npcs <spawnall|despawn|reload|list>")
                    .color(TextColor.color(0xAAAAAA)));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawnall" -> {
                manager.spawnAll();
                sender.sendMessage(Component.text("NPCs spawned.")
                        .color(TextColor.color(0x55FF55)));
            }
            case "despawn" -> {
                manager.despawnAll();
                sender.sendMessage(Component.text("NPCs despawned.")
                        .color(TextColor.color(0x55FF55)));
            }
            case "reload" -> {
                manager.despawnAll();
                manager.loadAll();
                manager.spawnAll();
                sender.sendMessage(Component.text("NPCs reloaded and spawned.")
                        .color(TextColor.color(0x55FF55)));
            }
            case "list" -> {
                sender.sendMessage(Component.text("NPCs configured: " + manager.getNpcCount())
                        .color(TextColor.color(0xAAAAAA)));
            }
            default -> sender.sendMessage(Component.text("Usage: /npcs <spawnall|despawn|reload|list>")
                    .color(TextColor.color(0xAAAAAA)));
        }

        return true;
    }
}
