package gg.magic.academy.core.mana;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.core.player.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ManaSystem {

    private static final int REGEN_AMOUNT = 5;   // mana per tick
    private static final long REGEN_INTERVAL = 20L; // ticks (1 second)

    private final JavaPlugin plugin;
    private final PlayerDataManager playerData;

    public ManaSystem(JavaPlugin plugin, PlayerDataManager playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    public void startRegenTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                MagicPlayerData data = playerData.get(player);
                if (data == null) continue;
                if (data.getMana() < data.getMaxMana()) {
                    data.restoreMana(REGEN_AMOUNT);
                    sendManaActionBar(player, data);
                }
            }
        }, REGEN_INTERVAL, REGEN_INTERVAL);
    }

    /**
     * Attempts to consume mana for a spell cast. Returns false if insufficient mana.
     */
    public boolean consumeMana(Player player, int amount) {
        MagicPlayerData data = playerData.get(player);
        if (data == null) return false;
        boolean success = data.drainMana(amount);
        if (!success) {
            player.sendActionBar(Component.text("Not enough mana! (" + data.getMana() + "/" + data.getMaxMana() + ")")
                    .color(TextColor.color(0x5555FF)));
        } else {
            sendManaActionBar(player, data);
        }
        return success;
    }

    private void sendManaActionBar(Player player, MagicPlayerData data) {
        int mana = data.getMana();
        int max = data.getMaxMana();
        String bar = buildManaBar(mana, max, 20);
        player.sendActionBar(Component.text("✦ " + bar + " " + mana + "/" + max)
                .color(TextColor.color(0x5599FF)));
    }

    private String buildManaBar(int current, int max, int barLength) {
        int filled = (int) ((double) current / max * barLength);
        return "█".repeat(filled) + "░".repeat(barLength - filled);
    }
}
