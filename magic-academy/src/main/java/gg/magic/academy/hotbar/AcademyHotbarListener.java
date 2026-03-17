package gg.magic.academy.hotbar;

import gg.magic.academy.MagicAcademyPlugin;
import gg.magic.academy.api.player.MagicPlayerData;
import gg.magic.academy.contract.ContractBoardMenu;
import gg.magic.academy.core.player.PlayerDataManager;
import gg.magic.academy.core.stat.StatEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class AcademyHotbarListener implements Listener {

    private final ContractBoardMenu contractBoardMenu;
    private final NamespacedKey actionKey;

    public AcademyHotbarListener(ContractBoardMenu contractBoardMenu) {
        this.contractBoardMenu = contractBoardMenu;
        this.actionKey = new NamespacedKey(
                gg.magic.academy.core.hotbar.LobbyHotbarManager.PDC_NAMESPACE,
                gg.magic.academy.core.hotbar.LobbyHotbarManager.PDC_ACTION_KEY);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        var item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String action = item.getItemMeta().getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "contracts" -> { event.setCancelled(true); contractBoardMenu.open(player, 0); }
            case "profile"   -> { event.setCancelled(true); showProfile(player); }
        }
    }

    private void showProfile(Player player) {
        PlayerDataManager pdm = (PlayerDataManager) MagicAcademyPlugin.getCoreAPI().getPlayerDataManager();
        MagicPlayerData data = pdm.get(player);
        if (data == null) return;

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("§8§m──────────────────────────────"));
        player.sendMessage(Component.text("§6✦ §eProfile: §f" + player.getName()));
        player.sendMessage(Component.text("§6  Rank: §e" + data.getRank().name().replace('_', ' ')));
        StatEngine statEngine = (StatEngine) MagicAcademyPlugin.getCoreAPI().getStatEngine();
        int maxMana = statEngine.computeMaxMana(data);
        player.sendMessage(Component.text("§6  Mana: §b" + data.getMana() + " §7/ §b" + maxMana));
        player.sendMessage(Component.text("§6  Spells owned: §e" + data.getSpellTiers().size()));
        player.sendMessage(Component.text("§6  Artifacts: §e" + data.getOwnedArtifacts().size()
                + " §7(active: §e" + data.getActiveArtifacts().size() + "§7)"));
        player.sendMessage(Component.text("§8§m──────────────────────────────"));
    }
}
