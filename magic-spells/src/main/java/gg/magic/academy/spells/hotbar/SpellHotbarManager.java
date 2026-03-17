package gg.magic.academy.spells.hotbar;

import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.spells.MagicSpells;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives players their equipped spell items in hotbar slots 0–3 when they enter
 * a dungeon or trial world. Reverts automatically when LobbyHotbarManager
 * restores the hub hotbar on returning to the hub world.
 *
 * Combat hotbar layout:
 *   Slots 0–3 — Equipped spell items (BOOK with CMD texture, PDC spell_slot tag)
 *   Slots 4–8 — Untouched (cleared/left empty)
 *
 * World rules:
 *   dungeon_*     → spell hotbar
 *   academy_trials → spell hotbar
 *   everything else → no action here (LobbyHotbarManager handles hub)
 */
public class SpellHotbarManager implements Listener {

    public static final NamespacedKey SPELL_SLOT_KEY = new NamespacedKey("magic", "spell_slot");

    private final JavaPlugin plugin;

    public SpellHotbarManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ── World-change trigger ───────────────────────────────────────────────────

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        String newWorld = event.getPlayer().getWorld().getName();
        if (isCombatWorld(newWorld)) {
            giveSpellHotbar(event.getPlayer());
        }
    }

    /** Handle players who log back in while still inside a dungeon/trial world. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String world = event.getPlayer().getWorld().getName();
        if (isCombatWorld(world)) {
            // Run next tick so LobbyHotbarManager's onJoin has already fired first
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> giveSpellHotbar(event.getPlayer()));
        }
    }

    // ── Drop prevention ────────────────────────────────────────────────────────

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (!item.hasItemMeta()) return;
        String tag = item.getItemMeta().getPersistentDataContainer()
                .get(SPELL_SLOT_KEY, PersistentDataType.STRING);
        if (tag != null) event.setCancelled(true);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Populate hotbar slots 0–3 with the player's equipped spell items.
     * Empty spell slots get a placeholder item.
     */
    public void giveSpellHotbar(Player player) {
        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;

        List<String> equipped = data.getEquippedSpells();
        for (int i = 0; i < 4; i++) {
            String spellId = equipped.get(i);
            if (spellId == null) {
                player.getInventory().setItem(i, buildEmptySlot(i));
            } else {
                final int slot = i;
                MagicSpells.get().getSpellRegistry().get(spellId).ifPresent(spell -> {
                    int tier = data.getSpellTier(spell.id());
                    player.getInventory().setItem(slot, buildSpellItem(spell, tier));
                });
            }
        }
    }

    // ── Item builders ──────────────────────────────────────────────────────────

    private ItemStack buildSpellItem(SpellTemplate spell, int tier) {
        TextColor nameColor = switch (tier) {
            case 2 -> TextColor.color(0x55FF55);
            case 3 -> TextColor.color(0xFFFF55);
            case 4 -> TextColor.color(0xFF5555);
            default -> TextColor.color(0x55FFFF);
        };

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(spell.element().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        spell.tier(tier).ifPresent(t -> {
            lore.add(Component.text("Mana: " + t.manaCost())
                    .color(TextColor.color(0x5599FF)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cooldown: " + (t.cooldownMs() / 1000.0) + "s")
                    .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));
        });
        lore.add(Component.empty());
        lore.add(Component.text("Press F to cast")
                .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false));

        ItemStack stack = new ItemStack(Material.BOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(spell.name() + " [T" + tier + "]")
                .color(nameColor).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setCustomModelData(spellCmd(tier));
        meta.getPersistentDataContainer().set(SPELL_SLOT_KEY, PersistentDataType.STRING, spell.id());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildEmptySlot(int slot) {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Slot " + (slot + 1) + " — Empty")
                .color(TextColor.color(0x555555)).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Equip a spell via /spellbook")
                        .color(TextColor.color(0x444444)).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(SPELL_SLOT_KEY, PersistentDataType.STRING, "empty_" + slot);
        stack.setItemMeta(meta);
        return stack;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static boolean isCombatWorld(String worldName) {
        return worldName.startsWith("dungeon_") || worldName.equals("academy_trials");
    }

    private static int spellCmd(int tier) {
        return switch (tier) {
            case 2 -> 4002;
            case 3 -> 4003;
            case 4 -> 4004;
            default -> 4001;
        };
    }
}
