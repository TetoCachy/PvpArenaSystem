package com.tetocachy.pvparenasystem.duel;

import java.util.UUID;

public class DuelChallenge {
    private final UUID sender;
    private final UUID target;
    private final String kitName;
    private final String arenaName;
    private final int rounds;
    private final long createdAt;

    public DuelChallenge(UUID sender, UUID target, String kitName, String arenaName, int rounds) {
        this.sender = sender;
        this.target = target;
        this.kitName = kitName;
        this.arenaName = arenaName;
        this.rounds = rounds;
        this.createdAt = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > 60_000;
    }

    public UUID getSender() { return sender; }
    public UUID getTarget() { return target; }
    public String getKitName() { return kitName; }
    public String getArenaName() { return arenaName; }
    public int getRounds() { return rounds; }
}