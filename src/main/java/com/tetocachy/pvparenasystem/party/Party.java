package com.tetocachy.pvparenasystem.party;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class Party {
    private final UUID leader;
    private String name;
    private int maxMembers = 8;
    private boolean isPublic = false;
    private final List<UUID> members = new ArrayList<>();

    public Party(UUID leader, String leaderName) {
        this.leader = leader;
        this.name = leaderName + "'s Party";
        this.members.add(leader);
    }

    public boolean addMember(UUID uuid) {
        if (members.size() < maxMembers && !members.contains(uuid)) {
            members.add(uuid);
            return true;
        }
        return false;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public void broadcast(MinecraftServer server, String message) {
        Component comp = Component.literal(message);
        for (UUID u : members) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) p.sendSystemMessage(comp, false);
        }
    }

    public UUID getLeader() { return leader; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = Math.max(2, Math.min(32, maxMembers)); }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public List<UUID> getMembers() { return members; }
}