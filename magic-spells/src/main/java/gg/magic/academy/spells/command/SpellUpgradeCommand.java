package gg.magic.academy.spells.command;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.spells.MagicSpells;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SpellUpgradeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        MagicSpells spells = MagicSpells.get();
        if (spells == null) {
            player.sendMessage(Component.text("Spell system not available.")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        int heldSlot = player.getInventory().getHeldItemSlot(); // 0-8
        if (heldSlot > 3) {
            player.sendMessage(Component.text("Hold a spell in hotbar slot 1-4 to upgrade.")
                    .color(TextColor.color(0xFFAA00)));
            return true;
        }

        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return true;

        String spellId = data.getEquippedSpells().get(heldSlot);
        if (spellId == null) {
            player.sendMessage(Component.text("No spell equipped in slot " + (heldSlot + 1) + ".")
                    .color(TextColor.color(0xAAAAAA)));
            return true;
        }

        Optional<SpellTemplate> spellOpt = spells.getSpellRegistry().get(spellId);
        if (spellOpt.isEmpty()) {
            player.sendMessage(Component.text("Unknown spell: " + spellId)
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        spells.getUpgradeManager().upgrade(player, data, spellOpt.get());
        return true;
    }
}
