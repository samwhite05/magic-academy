package gg.magic.academy.core.hotbar;

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

import java.util.List;

/**
 * Gives players a fixed hotbar in the hub world containing clickable GUI shortcuts.
 *
 * Items use PDC tag {@code magic:hotbar_action} to identify their action.
 * Each plugin that owns a GUI registers a {@link PlayerInteractEvent} listener
 * which reads this tag and opens the appropriate menu.
 *
 * Hotbar layout:
 *   Slot 0 — Spellbook        (opens spell loadout)
 *   Slot 1 — Discoveries      (opens discoveries browser)
 *   Slot 2 — Contract Board   (opens contract GUI)
 *   Slot 3 — Hideout          (visits player hideout)
 *   Slot 8 — Player Profile   (shows rank / stats in chat)
 */
public class LobbyHotbarManager implements Listener {

    public static final String PDC_NAMESPACE = "magic";
    public static final String PDC_ACTION_KEY = "hotbar_action";

    private final JavaPlugin plugin;
    private final NamespacedKey actionKey;

    /** The name of the main hub world where hotbar items are given. */
    private static final String HUB_WORLD = "world";

    public LobbyHotbarManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(PDC_NAMESPACE, PDC_ACTION_KEY);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        giveHotbar(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        String newWorld = event.getPlayer().getWorld().getName();
        if (HUB_WORLD.equals(newWorld)) {
            giveHotbar(event.getPlayer());
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemMeta meta = event.getItemDrop().getItemStack().getItemMeta();
        if (meta == null) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            event.setCancelled(true);
        }
    }

    public void giveHotbar(Player player) {
        player.getInventory().setItem(0, buildItem(Material.ENCHANTED_BOOK, "§bSpellbook",
                "spellbook", List.of("§7View and equip your spells", "§7Right-click to open")));
        player.getInventory().setItem(1, buildItem(Material.BOOKSHELF, "§eDiscoveries",
                "discoveries", List.of("§7Browse all spell discoveries", "§7Right-click to open")));
        player.getInventory().setItem(2, buildItem(Material.WRITABLE_BOOK, "§6Contract Board",
                "contracts", List.of("§7Accept and track contracts", "§7Right-click to open")));
        player.getInventory().setItem(3, buildItem(Material.ENDER_EYE, "§dHideout",
                "hideout", List.of("§7Visit your personal hideout", "§7Right-click to travel")));
        player.getInventory().setItem(8, buildItem(Material.NETHER_STAR, "§fProfile",
                "profile", List.of("§7View your rank and stats", "§7Right-click to view")));
    }

    public ItemStack buildItem(Material material, String name, String action, List<String> loreStrings) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = loreStrings.stream()
                .map(l -> (Component) Component.text(l)
                        .color(TextColor.color(0xAAAAAA))
                        .decoration(TextDecoration.ITALIC, false))
                .toList();
        meta.lore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Returns the NamespacedKey used for the hotbar action PDC tag. */
    public NamespacedKey getActionKey() { return actionKey; }

    /**
     * Helper for other plugins to read the hotbar action from an item.
     * Returns null if the item has no hotbar action.
     */
    public static String getAction(ItemStack stack, NamespacedKey key) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
