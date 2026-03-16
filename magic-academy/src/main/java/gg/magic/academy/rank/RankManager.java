package gg.magic.academy.rank;

import gg.magic.academy.api.AcademyRank;
import gg.magic.academy.api.event.RankAdvanceEvent;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads rank gate requirements from academy/ranks.yml and evaluates advancement conditions.
 *
 * YAML format (plugins/MagicAcademy/academy/ranks.yml):
 *   APPRENTICE:
 *     required_dungeon_clears: [wizard_ruins]
 *     required_spells: [firebolt]
 *     required_discoveries: 1
 *     trial: trial_apprentice
 *   MAGE:
 *     required_dungeon_clears: [wizard_ruins, crystal_caverns]
 *     required_spells: [firebolt, ice_spike]
 *     required_discoveries: 3
 *     trial: trial_mage
 */
public class RankManager {

    private final JavaPlugin plugin;
    private final Map<AcademyRank, RankGate> gates = new HashMap<>();

    public RankManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadGates();
    }

    private void loadGates() {
        gates.clear();
        File file = new File(plugin.getDataFolder().getParentFile().getParentFile(),
                "plugins/MagicAcademy/academy/ranks.yml");
        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String rankName : cfg.getKeys(false)) {
            try {
                AcademyRank rank = AcademyRank.valueOf(rankName.toUpperCase());
                ConfigurationSection s = cfg.getConfigurationSection(rankName);
                if (s == null) continue;
                gates.put(rank, new RankGate(
                        rankName,
                        s.getStringList("required_dungeon_clears"),
                        s.getStringList("required_spells"),
                        s.getInt("required_discoveries", 0),
                        s.getString("trial", "")
                ));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load rank gate: " + rankName, e);
            }
        }
    }

    /**
     * Checks if all non-trial requirements are met for the next rank.
     * The trial itself is gated separately — player must speak to NPC then pass trial.
     */
    public boolean meetsPreTrialRequirements(Player player, MagicPlayerData data) {
        AcademyRank nextRank = data.getRank().next();
        if (nextRank == data.getRank()) return false; // already at max

        RankGate gate = gates.get(nextRank);
        if (gate == null) return true; // no gate = free advancement

        var db = MagicCore.get().getDatabaseManager();

        // Dungeon clears
        for (String dungeonId : gate.requiredDungeonClears()) {
            if (db.getDungeonClearCount(player.getUniqueId(), dungeonId) < 1) {
                player.sendMessage(Component.text("✦ You must clear " + dungeonId + " to advance.")
                        .color(TextColor.color(0xFF5555)));
                return false;
            }
        }

        // Required spells
        for (String spellId : gate.requiredSpells()) {
            if (!data.hasSpell(spellId)) {
                player.sendMessage(Component.text("✦ You must learn the spell: " + spellId + " to advance.")
                        .color(TextColor.color(0xFF5555)));
                return false;
            }
        }

        return true;
    }

    /** Called after a player passes their trial. Advances rank. */
    public void advanceRank(Player player, MagicPlayerData data) {
        AcademyRank prev = data.getRank();
        AcademyRank next = prev.next();
        if (next == prev) return;

        data.setRank(next);

        // Update LuckPerms group if installed
        trySetLuckPermsGroup(player, next);

        plugin.getServer().getPluginManager().callEvent(new RankAdvanceEvent(player, prev, next));

        // Announce
        String announcement = "§6✦ §e" + player.getName() + " §6has advanced to §e" + next.name() + "§6!";
        plugin.getServer().broadcast(Component.text(announcement));
    }

    private void trySetLuckPermsGroup(Player player, AcademyRank rank) {
        try {
            Class<?> lpClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object lp = lpClass.getMethod("get").invoke(null);
            Object userManager = lp.getClass().getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
            if (user == null) return;
            Object data = user.getClass().getMethod("data").invoke(user);
            // Set primary group to rank name (lowercase)
            data.getClass().getMethod("addNode", Class.forName("net.luckperms.api.node.Node"))
                    .invoke(data, Class.forName("net.luckperms.api.node.types.InheritanceNode")
                            .getMethod("builder", String.class)
                            .invoke(null, rank.name().toLowerCase())
                    );
            userManager.getClass().getMethod("saveUser", Class.forName("net.luckperms.api.model.user.User"))
                    .invoke(userManager, user);
        } catch (Exception e) {
            // LuckPerms not installed or reflection failed — skip
        }
    }

    public Optional<RankGate> getGate(AcademyRank rank) { return Optional.ofNullable(gates.get(rank)); }
}
