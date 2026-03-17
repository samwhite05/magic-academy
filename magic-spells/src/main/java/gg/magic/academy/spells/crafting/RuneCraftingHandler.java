package gg.magic.academy.spells.crafting;

import gg.magic.academy.api.event.RuneDiscoveryEvent;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.api.rune.RuneTemplate;
import gg.magic.academy.api.rune.RuneType;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.core.database.DatabaseManager;
import gg.magic.academy.items.registry.ItemRegistry;
import gg.magic.academy.spells.registry.RuneRegistry;
import gg.magic.academy.spells.registry.SpellRegistry;
import gg.magic.academy.spells.upgrade.SpellUpgradeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Intercepts the vanilla crafting table to detect rune combinations.
 *
 * Pattern: Player places exactly 3 runes in the crafting slots —
 *          one ELEMENT, one SHAPE, one EFFECT rune — then clicks result slot.
 */
public class RuneCraftingHandler implements Listener {

    private final JavaPlugin plugin;
    private final SpellRegistry spellRegistry;
    private final RuneRegistry runeRegistry;
    private final SpellUpgradeManager upgradeManager;

    private static final int MAX_CRAFTS_PER_MINUTE = 10;

    public RuneCraftingHandler(JavaPlugin plugin, SpellRegistry spellRegistry,
                                RuneRegistry runeRegistry, SpellUpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.runeRegistry = runeRegistry;
        this.upgradeManager = upgradeManager;
    }

    @EventHandler
    public void onCraft(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.WORKBENCH) return;
        if (!(event.getInventory() instanceof CraftingInventory crafting)) return;

        // Only care about the result slot (slot 0 in workbench)
        if (event.getRawSlot() != 0) return;

        ItemStack[] matrix = crafting.getMatrix();
        RuneTemplate element = null, shape = null, effect = null;

        for (ItemStack stack : matrix) {
            String itemId = ItemRegistry.getItemId(stack);
            if (itemId == null) continue;
            Optional<RuneTemplate> runeOpt = runeRegistry.get(itemId);
            if (runeOpt.isEmpty()) continue;
            RuneTemplate rune = runeOpt.get();
            switch (rune.type()) {
                case ELEMENT -> element = rune;
                case SHAPE -> shape = rune;
                case EFFECT -> effect = rune;
            }
        }

        // Must have all 3 types and no extra items
        if (element == null || shape == null || effect == null) return;

        // Count total items in matrix — should be exactly 3
        int total = 0;
        for (ItemStack stack : matrix) {
            if (stack != null && !stack.getType().isAir()) total += stack.getAmount();
        }
        if (total != 3) return;

        event.setCancelled(true);

        // Rate limit check
        DatabaseManager db = MagicCore.getInstance().getDatabaseManager();
        if (!db.checkCraftingRateLimit(player.getUniqueId(), MAX_CRAFTS_PER_MINUTE)) {
            player.sendMessage(Component.text("You're crafting too fast. Slow down!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        MagicPlayerData data = MagicCore.getInstance().getPlayerDataManager().get(player);
        if (data == null) return;

        String elementId = element.id();
        String shapeId = shape.id();
        String effectId = effect.id();

        Optional<String> recipeResult = runeRegistry.resolveRecipe(elementId, shapeId, effectId);

        if (recipeResult.isEmpty()) {
            player.sendMessage(Component.text("✦ These runes resonate... but produce nothing. Keep experimenting.")
                    .color(TextColor.color(0xAA00AA)));
            consumeRunes(crafting, player);
            return;
        }

        String spellId = recipeResult.get();
        Optional<SpellTemplate> spellOpt = spellRegistry.get(spellId);
        if (spellOpt.isEmpty()) return;
        SpellTemplate spell = spellOpt.get();

        // Check if this is a first-time discovery
        boolean isFirstDiscovery = !db.isDiscovered(spellId);

        if (isFirstDiscovery) {
            db.recordDiscovery(spellId, player.getUniqueId(), player.getName());
            RuneDiscoveryEvent discoveryEvent = new RuneDiscoveryEvent(player, spell, elementId, shapeId, effectId);
            plugin.getServer().getPluginManager().callEvent(discoveryEvent);

            // Announce server-wide
            String announcement = "§6✦ §e" + player.getName() + " §6has discovered the spell §e" + spell.name() + "§6!";
            plugin.getServer().broadcast(Component.text(announcement));
        }

        // Grant the spell if they don't have it
        if (!data.hasSpell(spellId)) {
            data.grantSpell(spellId);
            player.sendMessage(Component.text("✦ You have learned " + spell.name() + "!")
                    .color(TextColor.color(0x55FF55)));
        } else {
            player.sendMessage(Component.text("✦ You already know " + spell.name() + ".")
                    .color(TextColor.color(0xAAAAAA)));
        }

        consumeRunes(crafting, player);
    }

    private void consumeRunes(CraftingInventory crafting, Player player) {
        ItemStack[] matrix = crafting.getMatrix();
        for (ItemStack stack : matrix) {
            if (stack != null && !stack.getType().isAir()) {
                stack.setAmount(stack.getAmount() - 1);
            }
        }
        crafting.setMatrix(matrix);
    }
}
