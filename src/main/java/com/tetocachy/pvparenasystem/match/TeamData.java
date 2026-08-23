package com.tetocachy.pvparenasystem.match;

import net.minecraft.ChatFormatting;

import java.util.*;

public class TeamData {
    private final int teamIndex;
    private final String name;
    private final ChatFormatting color;
    private final List<UUID> members = new ArrayList<>();
    private final Set<UUID> aliveMembers = new HashSet<>();
    private int score = 0;

    public TeamData(int teamIndex, String name, ChatFormatting color, List<UUID> initialMembers) {
        this.teamIndex = teamIndex;
        this.name = name;
        this.color = color;
        this.members.addAll(initialMembers);
        this.aliveMembers.addAll(initialMembers);
    }

    public void resetRound() {
        aliveMembers.clear();
        aliveMembers.addAll(members);
    }

    public void markEliminated(UUID uuid) {
        aliveMembers.remove(uuid);
    }

    public boolean isAlive(UUID uuid) {
        return aliveMembers.contains(uuid);
    }

    public boolean isTeamEliminated() {
        return aliveMembers.isEmpty();
    }

    public int getTeamIndex() { return teamIndex; }
    public String getName() { return name; }
    public ChatFormatting getColor() { return color; }
    public List<UUID> getMembers() { return members; }
    public Set<UUID> getAliveMembers() { return aliveMembers; }
    public int getScore() { return score; }
    public void incrementScore() { this.score++; }
}