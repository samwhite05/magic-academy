package gg.magic.academy.npcs.command;

import gg.magic.academy.npcs.MagicNpcs;
import gg.magic.academy.npcs.npc.NpcDefinition;
import gg.magic.academy.npcs.npc.NpcManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawnall" -> {
                manager.spawnAll();
                sender.sendMessage(Component.text("All NPCs spawned.")
                        .color(TextColor.color(0x55FF55)));
            }
            case "despawn" -> {
                manager.despawnAll();
                sender.sendMessage(Component.text("All NPCs despawned.")
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
                sender.sendMessage(Component.text("=== NPCs ===")
                        .color(TextColor.color(0xFFAA00)));
                for (NpcDefinition def : manager.getAllNPCs()) {
                    Component line = Component.text("  - ")
                            .color(TextColor.color(0xAAAAAA))
                            .append(Component.text(def.name())
                                    .color(TextColor.color(0x55FF55)))
                            .append(Component.text(" [" + def.id() + "]")
                                    .color(TextColor.color(0x888888)))
                            .append(Component.text(" @ " + def.worldName() + ":" +
                                    (int) def.x() + "," + (int) def.y() + "," + (int) def.z())
                                    .color(TextColor.color(0x666666)));
                    sender.sendMessage(line);
                }
                sender.sendMessage(Component.text("Total: " + manager.getNpcCount() + " NPCs")
                        .color(TextColor.color(0xAAAAAA)));
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /npcs info <npc-id>")
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                NpcDefinition def = manager.getNPC(args[1]);
                if (def == null) {
                    sender.sendMessage(Component.text("NPC not found: " + args[1])
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                sender.sendMessage(Component.text("=== NPC Info: " + def.id() + " ===")
                        .color(TextColor.color(0xFFAA00)));
                sender.sendMessage(Component.text("Name: " + def.name()).color(TextColor.color(0x55FF55)));
                sender.sendMessage(Component.text("World: " + def.worldName()).color(TextColor.color(0x55FF55)));
                sender.sendMessage(Component.text("Location: " + def.x() + ", " + def.y() + ", " + def.z())
                        .color(TextColor.color(0x55FF55)));
                sender.sendMessage(Component.text("Yaw: " + def.yaw()).color(TextColor.color(0x55FF55)));
                sender.sendMessage(Component.text("Type: " + def.interactionType().name())
                        .color(TextColor.color(0x55FF55)));
                if (!def.interactionTarget().isEmpty()) {
                    sender.sendMessage(Component.text("Target: " + def.interactionTarget())
                            .color(TextColor.color(0x55FF55)));
                }
            }
            case "spawn" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /npcs spawn <npc-id>")
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                NpcDefinition def = manager.getNPC(args[1]);
                if (def == null) {
                    sender.sendMessage(Component.text("NPC not found: " + args[1])
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                manager.spawn(def.id());
                sender.sendMessage(Component.text("NPC '" + def.name() + "' spawned.")
                        .color(TextColor.color(0x55FF55)));
            }
            case "tp", "teleport" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command can only be used by players.")
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /npcs tp <npc-id>")
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                NpcDefinition def = manager.getNPC(args[1]);
                if (def == null) {
                    sender.sendMessage(Component.text("NPC not found: " + args[1])
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                player.teleport(def.getLocation());
                sender.sendMessage(Component.text("Teleported to " + def.name())
                        .color(TextColor.color(0x55FF55)));
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command can only be used by players.")
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /npc setspawn <npc-id>")
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                NpcDefinition def = manager.getNPC(args[1]);
                if (def == null) {
                    sender.sendMessage(Component.text("NPC not found: " + args[1])
                            .color(TextColor.color(0xFF5555)));
                    return true;
                }
                manager.updateNPCLocation(args[1], player.getLocation());
                sender.sendMessage(Component.text("NPC '" + def.name() + "' moved to your location.")
                        .color(TextColor.color(0x55FF55)));
                sender.sendMessage(Component.text("New location: " +
                                (int) player.getX() + "," + (int) player.getY() + "," + (int) player.getZ())
                        .color(TextColor.color(0x888888)));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== NPC Commands ===")
                .color(TextColor.color(0xFFAA00)));
        sender.sendMessage(Component.text("/npcs list - Show all NPCs")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npcs info <id> - Show NPC details")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npcs spawn <id> - Spawn single NPC")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npcs tp <id> - Teleport to NPC")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npc setspawn <id> - Move NPC to your position")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npcs spawnall - Spawn all NPCs")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npcs despawn - Despawn all NPCs")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("/npcs reload - Reload and spawn all")
                .color(TextColor.color(0x55FF55)));
    }
}
