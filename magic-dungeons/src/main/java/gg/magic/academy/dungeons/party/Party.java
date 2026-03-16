package gg.magic.academy.dungeons.party;

import java.util.*;

/**
 * Represents a group of players queued or running a dungeon together.
 */
public class Party {

    private final UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID getLeader() { return leader; }

    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }

    public boolean addMember(UUID uuid) { return members.add(uuid); }
    public boolean removeMember(UUID uuid) { return members.remove(uuid); }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public int size() { return members.size(); }
}
