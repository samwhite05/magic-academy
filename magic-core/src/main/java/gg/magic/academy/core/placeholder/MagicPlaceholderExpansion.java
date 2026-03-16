package gg.magic.academy.core.placeholder;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.MagicCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for Magic Academy.
 *
 * Available placeholders:
 *   %magic_mana%       — current mana
 *   %magic_max_mana%   — max mana (including stat bonuses)
 *   %magic_rank%       — player rank name
 *   %magic_spell_1%    — spell in loadout slot 1 (or "Empty")
 *   %magic_spell_2%    — spell in loadout slot 2
 *   %magic_spell_3%    — spell in loadout slot 3
 *   %magic_spell_4%    — spell in loadout slot 4
 */
public class MagicPlaceholderExpansion extends PlaceholderExpansion {

    private final MagicCore plugin;

    public MagicPlaceholderExpansion(MagicCore plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "magic"; }
    @Override public @NotNull String getAuthor()     { return "MagicAcademyDev"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }
    @Override public boolean canRegister()           { return true; }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";

        MagicPlayerData data = plugin.getPlayerDataManager()
                .get(offlinePlayer.getPlayer());
        if (data == null) return "";

        return switch (params.toLowerCase()) {
            case "mana"     -> String.valueOf(data.getMana());
            case "max_mana" -> String.valueOf(
                    plugin.getStatEngine().computeMaxMana(data));
            case "rank"     -> data.getRank().name().replace('_', ' ');
            case "spell_1"  -> spellName(data, 0);
            case "spell_2"  -> spellName(data, 1);
            case "spell_3"  -> spellName(data, 2);
            case "spell_4"  -> spellName(data, 3);
            default -> null;
        };
    }

    private String spellName(MagicPlayerData data, int slot) {
        String id = data.getEquippedSpells().get(slot);
        return id != null ? id.replace('_', ' ') : "Empty";
    }
}
