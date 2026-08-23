package com.tetocachy.pvparenasystem.party;

import java.util.*;

public class Party {
    private final UUID leader;
    private final List<UUID> members = new ArrayList<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() { return leader; }
    public List<UUID> getMembers() { return members; }
    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
}