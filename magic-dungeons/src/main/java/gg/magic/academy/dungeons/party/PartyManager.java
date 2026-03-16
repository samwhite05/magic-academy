package gg.magic.academy.dungeons.party;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages party formation, invites, and membership.
 * Each player can be in at most one party at a time.
 */
public class PartyManager {

    private static final long INVITE_EXPIRY_MS = 60_000L;

    /** playerUUID -> their Party */
    private final Map<UUID, Party> playerParty = new ConcurrentHashMap<>();

    /** invitee UUID -> (inviter UUID, expiry timestamp) */
    private final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();

    private final JavaPlugin plugin;

    public PartyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Cleanup expired invites every 30 seconds
        plugin.getServer().getScheduler().runTaskTimer(plugin, () ->
                pendingInvites.entrySet().removeIf(e -> e.getValue().isExpired()), 600L, 600L);
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public Optional<Party> getParty(UUID uuid) {
        return Optional.ofNullable(playerParty.get(uuid));
    }

    /** Returns the party members including the player themselves, or a singleton list. */
    public List<UUID> getPartyMembers(UUID uuid) {
        Party p = playerParty.get(uuid);
        return p == null ? List.of(uuid) : new ArrayList<>(p.getMembers());
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    public void invite(Player inviter, Player target) {
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            inviter.sendMessage(msg("You can't invite yourself.", 0xFF5555));
            return;
        }
        if (playerParty.containsKey(target.getUniqueId())) {
            inviter.sendMessage(msg(target.getName() + " is already in a party.", 0xFF5555));
            return;
        }
        if (pendingInvites.containsKey(target.getUniqueId())) {
            inviter.sendMessage(msg(target.getName() + " already has a pending invite.", 0xAAAAAA));
            return;
        }

        // Ensure inviter has (or creates) a party
        playerParty.computeIfAbsent(inviter.getUniqueId(), k -> new Party(inviter.getUniqueId()));

        pendingInvites.put(target.getUniqueId(),
                new PendingInvite(inviter.getUniqueId(), System.currentTimeMillis() + INVITE_EXPIRY_MS));

        inviter.sendMessage(msg("✦ Party invite sent to " + target.getName() + ".", 0x55FF55));
        target.sendMessage(msg("✦ " + inviter.getName() + " invited you to a party! Type /party accept or /party decline.", 0xFFAA00));
    }

    public void acceptInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            player.sendMessage(msg("You have no pending party invite.", 0xFF5555));
            return;
        }
        if (playerParty.containsKey(player.getUniqueId())) {
            player.sendMessage(msg("You are already in a party.", 0xFF5555));
            return;
        }
        Party party = playerParty.get(invite.inviterUuid());
        if (party == null) {
            player.sendMessage(msg("The party no longer exists.", 0xFF5555));
            return;
        }
        party.addMember(player.getUniqueId());
        playerParty.put(player.getUniqueId(), party);

        broadcastToParty(party, "✦ " + player.getName() + " joined the party!", 0x55FF55);
    }

    public void declineInvite(Player player) {
        PendingInvite invite = pendingInvites.remove(player.getUniqueId());
        if (invite == null) {
            player.sendMessage(msg("You have no pending party invite.", 0xAAAAAA));
            return;
        }
        player.sendMessage(msg("Invite declined.", 0xAAAAAA));
        Player inviter = Bukkit.getPlayer(invite.inviterUuid());
        if (inviter != null) inviter.sendMessage(msg(player.getName() + " declined your invite.", 0xAAAAAA));
    }

    // ── Leave / Kick / Disband ────────────────────────────────────────────────

    public void leave(Player player) {
        Party party = playerParty.remove(player.getUniqueId());
        if (party == null) {
            player.sendMessage(msg("You are not in a party.", 0xAAAAAA));
            return;
        }
        party.removeMember(player.getUniqueId());
        player.sendMessage(msg("You left the party.", 0xAAAAAA));

        if (party.getMembers().isEmpty()) return;

        if (party.isLeader(player.getUniqueId())) {
            // Assign new leader (first remaining member)
            // Party is immutable leader, so we need to rebuild — simplest: disband
            disbandPartyObject(party, player.getName() + " (leader) left — party disbanded.");
        } else {
            broadcastToParty(party, "✦ " + player.getName() + " left the party.", 0xAAAAAA);
        }
    }

    public void kick(Player leader, String targetName) {
        Party party = playerParty.get(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(msg("You are not the party leader.", 0xFF5555));
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !party.isMember(target.getUniqueId())) {
            leader.sendMessage(msg(targetName + " is not in your party.", 0xFF5555));
            return;
        }
        party.removeMember(target.getUniqueId());
        playerParty.remove(target.getUniqueId());
        target.sendMessage(msg("You were kicked from the party.", 0xFF5555));
        broadcastToParty(party, "✦ " + targetName + " was kicked.", 0xAAAAAA);
    }

    public void disband(Player leader) {
        Party party = playerParty.get(leader.getUniqueId());
        if (party == null || !party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(msg("You are not the party leader.", 0xFF5555));
            return;
        }
        disbandPartyObject(party, "Party disbanded by leader.");
    }

    private void disbandPartyObject(Party party, String reason) {
        for (UUID uuid : party.getMembers()) {
            playerParty.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg("✦ " + reason, 0xAAAAAA));
        }
    }

    // ── Info ──────────────────────────────────────────────────────────────────

    public void showInfo(Player player) {
        Party party = playerParty.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(msg("You are not in a party. Use /party invite <player> to start one.", 0xAAAAAA));
            return;
        }
        player.sendMessage(msg("── Party (" + party.size() + " members) ──", 0xFFAA00));
        for (UUID uuid : party.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            String name = member != null ? member.getName() : uuid.toString().substring(0, 8) + "...";
            String tag = party.isLeader(uuid) ? " [Leader]" : "";
            player.sendMessage(msg("  " + name + tag, party.isLeader(uuid) ? 0xFFFF55 : 0xAAAAAA));
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    public void removePlayer(UUID uuid) {
        Party party = playerParty.remove(uuid);
        if (party != null) party.removeMember(uuid);
        pendingInvites.remove(uuid);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void broadcastToParty(Party party, String message, int color) {
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg(message, color));
        }
    }

    private Component msg(String text, int hex) {
        return Component.text(text).color(TextColor.color(hex));
    }

    private record PendingInvite(UUID inviterUuid, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }
}
