package com.tetocachy.pvparenasystem.lobby;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.match.MatchManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaLobby {
    private final UUID lobbyId = UUID.randomUUID();
    private final UUID hostUuid;
    private final String hostName;
    private Arena arena;
    private Kit kit;
    private int teamCount;
    private int playersPerTeam;
    private int rounds = 3;

    private final Map<Integer, List<UUID>> teamSlots = new ConcurrentHashMap<>();

    public ArenaLobby(UUID hostUuid, String hostName, Arena arena, Kit kit, int teamCount, int playersPerTeam, int rounds) {
        this.hostUuid = hostUuid;
        this.hostName = hostName;
        this.arena = arena;
        this.kit = kit;
        this.teamCount = Math.max(2, teamCount);
        this.playersPerTeam = Math.max(1, playersPerTeam);
        this.rounds = Math.max(1, rounds);

        for (int i = 1; i <= this.teamCount; i++) {
            teamSlots.put(i, new ArrayList<>());
        }
        joinTeam(hostUuid, 1);
    }

    public synchronized boolean joinTeam(UUID playerUuid, int targetTeam) {
        leave(playerUuid);
        List<UUID> slot = teamSlots.get(targetTeam);
        if (slot != null && slot.size() < playersPerTeam) {
            slot.add(playerUuid);
            return true;
        }
        return false;
    }

    public synchronized void leave(UUID playerUuid) {
        for (List<UUID> slot : teamSlots.values()) {
            slot.remove(playerUuid);
        }
    }

    public boolean canStart() {
        int occupiedTeams = 0;
        for (List<UUID> slot : teamSlots.values()) {
            if (!slot.isEmpty()) occupiedTeams++;
        }
        return occupiedTeams >= 2;
    }

    public void startMatch(MinecraftServer server) {
        if (arena == null) return;
        Map<Integer, List<UUID>> activeTeams = new HashMap<>();
        for (Map.Entry<Integer, List<UUID>> entry : teamSlots.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                activeTeams.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        broadcast(server, "§6§l[Lobby] §aStarting match in §e" + arena.getDisplayName() + "§a!");
        MatchManager.createMatch(server, arena, kit, rounds, activeTeams);
    }

    public void broadcast(MinecraftServer server, String message) {
        Component comp = Component.literal(message);
        for (List<UUID> slot : teamSlots.values()) {
            for (UUID u : slot) {
                ServerPlayer p = server.getPlayerList().getPlayer(u);
                if (p != null) p.sendSystemMessage(comp, false);
            }
        }
    }

    public UUID getLobbyId() { return lobbyId; }
    public UUID getHostUuid() { return hostUuid; }
    public String getHostName() { return hostName; }
    public Arena getArena() { return arena; }
    public Kit getKit() { return kit; }
    public int getTeamCount() { return teamCount; }
    public int getPlayersPerTeam() { return playersPerTeam; }
    public int getRounds() { return rounds; }
    public Map<Integer, List<UUID>> getTeamSlots() { return teamSlots; }
}