package gg.magic.academy.contract;

import gg.magic.academy.api.event.DungeonCompleteEvent;
import gg.magic.academy.api.event.SpellCastEvent;
import gg.magic.academy.core.MagicCore;
import gg.magic.academy.core.database.DatabaseManager;
import gg.magic.academy.items.MagicItems;
import gg.magic.academy.npcs.dialogue.DialogueQuestDispatchEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active contract progress in memory, persists to DB on change.
 * Listens for: DialogueQuestDispatchEvent, EntityDeathEvent, DungeonCompleteEvent, SpellCastEvent.
 */
public class ContractManager implements Listener {

    private final JavaPlugin plugin;
    private final ContractRegistry registry;

    /**
     * In-memory progress cache.
     * playerUUID -> contractId -> int[] progress per objective index
     */
    private final Map<UUID, Map<String, int[]>> progress = new ConcurrentHashMap<>();
    /** playerUUID -> set of active contractIds */
    private final Map<UUID, Set<String>> activeContracts = new ConcurrentHashMap<>();

    public ContractManager(JavaPlugin plugin, ContractRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    // ── Player session ────────────────────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        saveProgress(uuid);
        progress.remove(uuid);
        activeContracts.remove(uuid);
    }

    private void loadPlayer(UUID uuid) {
        DatabaseManager db = MagicCore.get().getDatabaseManager();
        List<String> active = db.getActiveContracts(uuid);
        Set<String> activeSet = new HashSet<>(active);
        activeContracts.put(uuid, activeSet);
        Map<String, int[]> playerProgress = new HashMap<>();
        for (String contractId : active) {
            registry.get(contractId).ifPresent(template -> {
                Map<Integer, Integer> dbProg = db.getContractProgress(uuid, contractId);
                int[] arr = new int[template.objectives().size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = dbProg.getOrDefault(i, 0);
                }
                playerProgress.put(contractId, arr);
            });
        }
        progress.put(uuid, playerProgress);
    }

    private void saveProgress(UUID uuid) {
        Map<String, int[]> playerProgress = progress.get(uuid);
        if (playerProgress == null) return;
        DatabaseManager db = MagicCore.get().getDatabaseManager();
        for (Map.Entry<String, int[]> entry : playerProgress.entrySet()) {
            String contractId = entry.getKey();
            int[] prog = entry.getValue();
            for (int i = 0; i < prog.length; i++) {
                db.setContractProgress(uuid, contractId, i, prog[i]);
            }
        }
    }

    // ── Contract activation ───────────────────────────────────────────────────

    /** Called from DialogueQuestDispatchEvent or ContractBoardMenu click. */
    public void startContract(Player player, String contractId) {
        UUID uuid = player.getUniqueId();
        Set<String> active = activeContracts.computeIfAbsent(uuid, k -> new HashSet<>());
        if (active.contains(contractId)) {
            player.sendMessage(Component.text("✦ You already have this contract active.")
                    .color(TextColor.color(0xAAAAAA)));
            return;
        }
        Optional<ContractTemplate> opt = registry.get(contractId);
        if (opt.isEmpty()) {
            player.sendMessage(Component.text("✦ Contract not found: " + contractId)
                    .color(TextColor.color(0xFF5555)));
            return;
        }
        List<String> completed = MagicCore.get().getDatabaseManager().getCompletedContracts(uuid);
        if (completed.contains(contractId)) {
            player.sendMessage(Component.text("✦ You have already completed that contract.")
                    .color(TextColor.color(0x888888)));
            return;
        }
        ContractTemplate template = opt.get();
        active.add(contractId);
        int[] arr = new int[template.objectives().size()];
        progress.computeIfAbsent(uuid, k -> new HashMap<>()).put(contractId, arr);
        MagicCore.get().getDatabaseManager().startContract(uuid, contractId);
        player.sendMessage(Component.text("✦ Contract accepted: " + template.name())
                .color(TextColor.color(0x55FF55)));
    }

    public boolean isActive(UUID uuid, String contractId) {
        Set<String> active = activeContracts.get(uuid);
        return active != null && active.contains(contractId);
    }

    public boolean isCompleted(UUID uuid, String contractId) {
        return MagicCore.get().getDatabaseManager().getCompletedContracts(uuid).contains(contractId);
    }

    public Map<String, int[]> getProgress(UUID uuid) {
        return Collections.unmodifiableMap(progress.getOrDefault(uuid, Map.of()));
    }

    // ── Event listeners ───────────────────────────────────────────────────────

    @EventHandler
    public void onQuestDispatch(DialogueQuestDispatchEvent event) {
        startContract(event.getPlayer(), event.getQuestId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        String entityType = event.getEntityType().name();
        increment(killer, ContractObjective.Type.KILL_MOB, entityType);
    }

    @EventHandler
    public void onDungeonComplete(DungeonCompleteEvent event) {
        increment(event.getPlayer(), ContractObjective.Type.COMPLETE_DUNGEON, event.getDungeonId());
    }

    @EventHandler
    public void onSpellCast(SpellCastEvent event) {
        if (event.isCancelled()) return;
        increment(event.getPlayer(), ContractObjective.Type.CAST_SPELL, event.getSpell().id());
    }

    private void increment(Player player, ContractObjective.Type type, String target) {
        UUID uuid = player.getUniqueId();
        Set<String> active = activeContracts.get(uuid);
        if (active == null || active.isEmpty()) return;

        Map<String, int[]> playerProgress = progress.get(uuid);
        if (playerProgress == null) return;

        for (String contractId : new HashSet<>(active)) {
            registry.get(contractId).ifPresent(template -> {
                int[] prog = playerProgress.get(contractId);
                if (prog == null) return;

                boolean anyChanged = false;
                for (int i = 0; i < template.objectives().size(); i++) {
                    ContractObjective obj = template.objectives().get(i);
                    if (obj.type() != type) continue;
                    if (!obj.target().equalsIgnoreCase(target)) continue;
                    if (prog[i] < obj.required()) {
                        prog[i]++;
                        MagicCore.get().getDatabaseManager()
                                .setContractProgress(uuid, contractId, i, prog[i]);
                        anyChanged = true;
                        player.sendMessage(Component.text("✦ " + obj.label() + ": "
                                + prog[i] + " / " + obj.required())
                                .color(TextColor.color(0xFFAA00)));
                    }
                }

                if (anyChanged && isContractComplete(template, prog)) {
                    completeContract(player, template);
                    active.remove(contractId);
                }
            });
        }
    }

    private boolean isContractComplete(ContractTemplate template, int[] prog) {
        for (int i = 0; i < template.objectives().size(); i++) {
            if (prog[i] < template.objectives().get(i).required()) return false;
        }
        return true;
    }

    private void completeContract(Player player, ContractTemplate template) {
        MagicCore.get().getDatabaseManager().completeContract(player.getUniqueId(), template.id());
        progress.getOrDefault(player.getUniqueId(), Map.of()).remove(template.id());

        player.sendMessage(Component.text("")); // spacer
        player.sendMessage(Component.text("§6✦ §eContract Complete: §6" + template.name()));
        player.sendMessage(Component.text("§6✦ §eRewards:"));

        // Give item rewards
        for (Map.Entry<String, Integer> entry : template.rewardItems().entrySet()) {
            MagicItems.get().getItemRegistry().get(entry.getKey(), entry.getValue())
                    .ifPresent(stack -> {
                        player.getInventory().addItem(stack);
                        player.sendMessage(Component.text("  + " + entry.getValue() + "x " + entry.getKey())
                                .color(TextColor.color(0xFFAA00)));
                    });
        }

        // Mana reward
        if (template.rewardMana() > 0) {
            MagicCore.get().getPlayerDataManager().get(player).restoreMana(template.rewardMana());
            player.sendMessage(Component.text("  + " + template.rewardMana() + " mana restored")
                    .color(TextColor.color(0x5599FF)));
        }

        plugin.getServer().broadcast(
                Component.text("§6✦ §e" + player.getName() + " §6completed the contract §e" + template.name() + "§6!"));
    }

    public ContractRegistry getRegistry() { return registry; }
}
