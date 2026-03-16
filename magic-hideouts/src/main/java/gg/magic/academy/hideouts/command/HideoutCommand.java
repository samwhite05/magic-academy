package gg.magic.academy.hideouts.command;

import gg.magic.academy.hideouts.manager.HideoutManager;
import gg.magic.academy.hideouts.menu.ArtifactDisplayMenu;
import gg.magic.academy.hideouts.menu.ModuleUpgradeMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HideoutCommand implements CommandExecutor {

    private final HideoutManager hideoutManager;
    private final ModuleUpgradeMenu upgradeMenu;
    private final ArtifactDisplayMenu artifactDisplayMenu;

    public HideoutCommand(HideoutManager hideoutManager,
                          ModuleUpgradeMenu upgradeMenu,
                          ArtifactDisplayMenu artifactDisplayMenu) {
        this.hideoutManager = hideoutManager;
        this.upgradeMenu = upgradeMenu;
        this.artifactDisplayMenu = artifactDisplayMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            hideoutManager.visitHideout(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "upgrade", "modules" -> upgradeMenu.open(player);
            case "artifacts" -> artifactDisplayMenu.open(player);
            case "leave", "exit" -> {
                World hubWorld = Bukkit.getWorld("world");
                Location target = hubWorld != null ? hubWorld.getSpawnLocation()
                        : Bukkit.getWorlds().get(0).getSpawnLocation();
                player.teleport(target);
                player.sendMessage(Component.text("Leaving hideout.")
                        .color(TextColor.color(0xAAAAAA)));
            }
            default -> player.sendMessage(Component.text("Usage: /hideout [upgrade|artifacts|leave]")
                    .color(TextColor.color(0xAAAAAA)));
        }
        return true;
    }
}
