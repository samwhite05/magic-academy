package gg.magic.academy.spells.discovery;

import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.core.database.DatabaseManager;
import gg.magic.academy.spells.registry.SpellRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Read-only paginated GUI showing all spells and their first-discoverer info.
 * 45 spell slots per page (5 rows) + 1 nav row at the bottom.
 */
public class DiscoveriesMenu {

    public static final String TITLE = "✦ Spell Discoveries";
    private static final int SPELLS_PER_PAGE = 45;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final JavaPlugin plugin;
    private final SpellRegistry spellRegistry;
    /** Used to tag prev/next buttons so the listener knows page direction. */
    public final NamespacedKey pageKey;

    public DiscoveriesMenu(JavaPlugin plugin, SpellRegistry spellRegistry) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.pageKey = new NamespacedKey(plugin, "discoveries_page");
    }

    public void open(Player player, int page) {
        List<SpellTemplate> allSpells = new ArrayList<>(spellRegistry.getAll());
        allSpells.sort(Comparator.comparing(SpellTemplate::name));

        Map<String, DatabaseManager.DiscoveryRecord> discoveredMap = new HashMap<>();
        for (DatabaseManager.DiscoveryRecord rec : MagicCore.get().getDatabaseManager().getAllDiscoveries()) {
            discoveredMap.put(rec.spellId(), rec);
        }

        int totalPages = Math.max(1, (int) Math.ceil(allSpells.size() / (double) SPELLS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE));

        int start = page * SPELLS_PER_PAGE;
        int end = Math.min(start + SPELLS_PER_PAGE, allSpells.size());

        for (int i = start; i < end; i++) {
            SpellTemplate spell = allSpells.get(i);
            DatabaseManager.DiscoveryRecord rec = discoveredMap.get(spell.id());
            inv.setItem(i - start, buildSpellItem(spell, rec));
        }

        // Bottom row (45–53): filler + prev + info + next
        ItemStack filler = buildGlass();
        for (int i = 45; i < 54; i++) inv.setItem(i, filler);

        if (page > 0) inv.setItem(45, buildNavButton("◀ Previous", page - 1));
        inv.setItem(49, buildInfoItem(page, totalPages, allSpells.size(), discoveredMap.size()));
        if (page < totalPages - 1) inv.setItem(53, buildNavButton("Next ▶", page + 1));

        player.openInventory(inv);
    }

    private ItemStack buildSpellItem(SpellTemplate spell, DatabaseManager.DiscoveryRecord rec) {
        Material mat = elementToMaterial(spell.element().name());
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();

        boolean discovered = rec != null;
        TextColor nameColor = discovered ? TextColor.color(0xFFAA00) : TextColor.color(0x555555);

        meta.displayName(Component.text(spell.name())
                .color(nameColor).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Element: " + spell.element().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shape: " + spell.shape().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Effect: " + spell.effect().name())
                .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Tiers: " + spell.maxTier())
                .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (discovered) {
            lore.add(Component.text("First discovered by:")
                    .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  " + rec.discovererName())
                    .color(TextColor.color(0xFFFF55)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  " + DATE_FMT.format(Instant.ofEpochMilli(rec.discoveredAt())))
                    .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Undiscovered")
                    .color(TextColor.color(0x555555)).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Combine runes to find it!")
                    .color(TextColor.color(0x444444)).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildNavButton(String label, int targetPage) {
        ItemStack stack = new ItemStack(Material.ARROW);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(label)
                .color(TextColor.color(0x00FF99)).decoration(TextDecoration.ITALIC, false));
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, targetPage);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildInfoItem(int page, int totalPages, int total, int discovered) {
        ItemStack stack = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Spell Discoveries")
                .color(TextColor.color(0xFFAA00)).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Page " + (page + 1) + " / " + totalPages)
                        .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false),
                Component.text("Discovered: " + discovered + " / " + total)
                        .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false)
        ));
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildGlass() {
        ItemStack stack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" "));
        stack.setItemMeta(meta);
        return stack;
    }

    private Material elementToMaterial(String element) {
        return switch (element) {
            case "FIRE" -> Material.BLAZE_POWDER;
            case "ICE" -> Material.PACKED_ICE;
            case "LIGHTNING" -> Material.LIGHTNING_ROD;
            case "SHADOW" -> Material.COAL;
            default -> Material.AMETHYST_SHARD;
        };
    }
}
