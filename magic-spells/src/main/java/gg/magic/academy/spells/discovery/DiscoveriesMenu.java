package gg.magic.academy.spells.discovery;

import gg.magic.academy.api.gui.GuiUtil;
import gg.magic.academy.api.gui.PagedGui;
import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.core.database.DatabaseManager;
import gg.magic.academy.spells.registry.SpellRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DiscoveriesMenu {

    public static final String TITLE = "✦ Spell Discoveries";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final SpellRegistry spellRegistry;

    public DiscoveriesMenu(SpellRegistry spellRegistry) {
        this.spellRegistry = spellRegistry;
    }

    public void open(Player player, int page) {
        List<SpellTemplate> allSpells = new ArrayList<>(spellRegistry.getAll());
        allSpells.sort(Comparator.comparing(SpellTemplate::name));

        Map<String, DatabaseManager.DiscoveryRecord> discoveredMap = new HashMap<>();
        for (DatabaseManager.DiscoveryRecord rec : MagicCore.getInstance().getDatabaseManager().getAllDiscoveries()) {
            discoveredMap.put(rec.spellId(), rec);
        }

        int totalSpells = allSpells.size();

        PagedGui gui = new PagedGui(Component.text(TITLE));

        // Info item at slot 40 (centre of nav row)
        gui.setFixed(40, GuiUtil.make(Material.KNOWLEDGE_BOOK,
                Component.text("Spell Discoveries")
                        .color(TextColor.color(0xFFAA00)).decoration(TextDecoration.ITALIC, false),
                List.of(
                        Component.text("Discovered: " + discoveredMap.size() + " / " + totalSpells)
                                .color(TextColor.color(0x55FF55)).decoration(TextDecoration.ITALIC, false)
                )));

        for (SpellTemplate spell : allSpells) {
            DatabaseManager.DiscoveryRecord rec = discoveredMap.get(spell.id());
            gui.addItem(buildSpellItem(spell, rec));
        }

        gui.open(player, page);
    }

    private ItemStack buildSpellItem(SpellTemplate spell, DatabaseManager.DiscoveryRecord rec) {
        boolean discovered = rec != null;
        TextColor nameColor = discovered ? TextColor.color(0xFFAA00) : TextColor.color(0x555555);

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

        // Use CMD 4001 (spell_tier_1 book texture) for discovered, 9002 (divider/dark) for undiscovered
        int cmd = discovered ? 4001 : 9002;
        Material mat = discovered ? Material.BOOK : Material.GRAY_STAINED_GLASS_PANE;
        return GuiUtil.make(mat, cmd,
                Component.text(spell.name()).color(nameColor).decoration(TextDecoration.ITALIC, false),
                lore);
    }
}
