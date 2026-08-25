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
    private int pointsToWin = 3;
    private boolean friendlyFire = false;

    private final Map<Integer, List<UUID>> teamSlots = new ConcurrentHashMap<>();
    private final Set<UUID> spectators = ConcurrentHashMap.newKeySet();

    public ArenaLobby(UUID hostUuid, String hostName, Arena arena, Kit kit, int teamCount, int playersPerTeam, int pointsToWin, boolean friendlyFire) {
        this.hostUuid = hostUuid;
        this.hostName = hostName;
        this.arena = arena;
        this.kit = kit;
        this.teamCount = Math.max(2, teamCount);
        this.playersPerTeam = Math.max(1, playersPerTeam);
        this.pointsToWin = Math.max(1, pointsToWin);
        this.friendlyFire = friendlyFire;

        for (int i = 1; i <= this.teamCount; i++) {
            teamSlots.put(i, new ArrayList<>());
        }
        joinTeam(hostUuid, 1);
    }

    public synchronized boolean joinTeam(UUID playerUuid, int targetTeam) {
        List<UUID> slot = teamSlots.get(targetTeam);
        if (slot != null && slot.size() < playersPerTeam) {
            leave(playerUuid);
            slot.add(playerUuid);
            return true;
        }
        return false;
    }

    public synchronized void joinSpectator(UUID playerUuid) {
        leave(playerUuid);
        spectators.add(playerUuid);
    }

    public synchronized int findAvailableTeam() {
        int bestTeam = -1;
        int minPlayers = Integer.MAX_VALUE;
        for (int i = 1; i <= teamCount; i++) {
            List<UUID> slot = teamSlots.get(i);
            if (slot != null && slot.size() < playersPerTeam) {
                if (slot.size() < minPlayers) {
                    minPlayers = slot.size();
                    bestTeam = i;
                }
            }
        }
        return bestTeam;
    }

    public synchronized int getTotalPlayerCount() {
        int total = 0;
        for (List<UUID> slot : teamSlots.values()) {
            total += slot.size();
        }
        return total;
    }

    public int getMaxCapacity() {
        return teamCount * playersPerTeam;
    }

    public boolean isFull() {
        return getTotalPlayerCount() >= getMaxCapacity();
    }

    public synchronized void leave(UUID playerUuid) {
        for (List<UUID> slot : teamSlots.values()) {
            slot.remove(playerUuid);
        }
        spectators.remove(playerUuid);
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
        broadcast(server, "§6§l[Lobby] §aStarting match in §e" + arena.getDisplayName() + " §7(First to " + pointsToWin + " Points)!");
        MatchManager.createMatch(server, arena, kit, pointsToWin, friendlyFire, activeTeams, new ArrayList<>(spectators));
    }

    public void broadcast(MinecraftServer server, String message) {
        Component comp = Component.literal(message);
        for (List<UUID> slot : teamSlots.values()) {
            for (UUID u : slot) {
                ServerPlayer p = server.getPlayerList().getPlayer(u);
                if (p != null) p.sendSystemMessage(comp, false);
            }
        }
        for (UUID u : spectators) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) p.sendSystemMessage(comp, false);
        }
    }

    public UUID getLobbyId() { return lobbyId; }
    public UUID getHostUuid() { return hostUuid; }
    public String getHostName() { return hostName; }
    public Arena getArena() { return arena; }
    public Kit getKit() { return kit; }
    public int getTeamCount() { return teamCount; }
    public int getPlayersPerTeam() { return playersPerTeam; }
    public int getPointsToWin() { return pointsToWin; }
    public boolean isFriendlyFire() { return friendlyFire; }
    public Map<Integer, List<UUID>> getTeamSlots() { return teamSlots; }
    public Set<UUID> getSpectators() { return spectators; }
}