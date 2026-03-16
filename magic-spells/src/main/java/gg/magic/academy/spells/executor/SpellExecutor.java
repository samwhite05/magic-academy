package gg.magic.academy.spells.executor;

import gg.magic.academy.api.event.SpellCastEvent;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.api.spell.SpellTier;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.spells.MagicSpells;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Listens for spell cast input (F key = swap hands) and executes the active spell.
 *
 * Spell slot activation:
 *   - Hotbar slot 1-4 correspond to spell loadout slots 0-3.
 *   - Player holds item in that slot and presses F to cast.
 *
 * Actual effect execution is dispatched to MythicMobs via its API,
 * or falls back to a built-in vanilla effect if no MythicMobs skill is configured.
 */
public class SpellExecutor implements Listener {

    private final JavaPlugin plugin;

    public SpellExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        int heldSlot = player.getInventory().getHeldItemSlot(); // 0-8

        // Only slots 0-3 are spell slots
        if (heldSlot > 3) return;

        event.setCancelled(true); // Don't actually swap

        MagicPlayerData data = MagicCore.get().getPlayerDataManager().get(player);
        if (data == null) return;

        String spellId = data.getEquippedSpells().get(heldSlot);
        if (spellId == null) {
            player.sendActionBar(Component.text("No spell in slot " + (heldSlot + 1))
                    .color(TextColor.color(0x888888)));
            return;
        }

        Optional<SpellTemplate> spellOpt = MagicSpells.get().getSpellRegistry().get(spellId);
        if (spellOpt.isEmpty()) return;

        SpellTemplate spell = spellOpt.get();
        int tier = data.getSpellTier(spellId);
        Optional<SpellTier> tierCfg = spell.tier(tier);
        if (tierCfg.isEmpty()) return;

        SpellTier tierData = tierCfg.get();

        // Cooldown check
        if (data.isOnCooldown(spellId, tierData.cooldownMs())) {
            long remainingMs = data.getCooldownRemainingMs(spellId, tierData.cooldownMs());
            long remainingSec = Math.max(1, (remainingMs + 999) / 1000);
            player.sendActionBar(Component.text(spell.name() + " on cooldown (" + remainingSec + "s)")
                    .color(TextColor.color(0xFF8800)));
            return;
        }

        // Mana check
        if (!MagicCore.get().getManaSystem().consumeMana(player, tierData.manaCost())) return;

        // Fire cancellable event (other plugins can cancel spell casts)
        SpellCastEvent castEvent = new SpellCastEvent(player, data, spell, tier);
        plugin.getServer().getPluginManager().callEvent(castEvent);
        if (castEvent.isCancelled()) return;

        // Set cooldown
        data.setCooldown(spellId);

        // Dispatch to MythicMobs or built-in fallback
        if (!tierData.mythicSkillId().isEmpty()) {
            dispatchMythicSkill(player, tierData.mythicSkillId());
        } else {
            dispatchFallback(player, spell, tier);
        }
    }

    private void dispatchMythicSkill(Player player, String skillId) {
        // MythicMobs API integration — cast skill from the player as caster
        // Wrapped in try-catch in case MythicMobs isn't installed
        try {
            Class<?> mythicClass = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper");
            Object helper = mythicClass.getMethod("getInstance").invoke(null);
            // MythicMobs 5.x: castSkill(LivingEntity, String, LivingEntity)
            mythicClass.getMethod("castSkill", org.bukkit.entity.LivingEntity.class, String.class,
                            org.bukkit.entity.LivingEntity.class)
                    .invoke(helper, player, skillId, player);
        } catch (Exception e) {
            plugin.getLogger().warning("MythicMobs not available for skill: " + skillId + " — using fallback.");
            dispatchFallback(player, null, 0);
        }
    }

    private void dispatchFallback(Player player, SpellTemplate spell, int tier) {
        // Vanilla particle burst as placeholder effect
        player.getWorld().spawnParticle(
                org.bukkit.Particle.FLAME,
                player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5)),
                30, 0.3, 0.3, 0.3, 0.05
        );
        if (spell != null) {
            player.sendActionBar(Component.text("✦ " + spell.name() + " (T" + tier + ")")
                    .color(TextColor.color(0xFF6600)));
        }
    }
}
