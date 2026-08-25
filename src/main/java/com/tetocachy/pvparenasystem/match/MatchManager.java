package com.tetocachy.pvparenasystem.match;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.kit.Kit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchManager {
    private static final Map<UUID, ArenaMatch> activeMatches = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToMatch = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> spectatorToMatch = new ConcurrentHashMap<>();

    public static ArenaMatch createMatch(MinecraftServer server, Arena arena, Kit kit, int pointsToWin, boolean friendlyFire, Map<Integer, List<UUID>> teamAssignments, List<UUID> spectators) {
        ArenaMatch match = new ArenaMatch(server, arena, kit, pointsToWin, friendlyFire, teamAssignments, spectators);
        activeMatches.put(match.getMatchId(), match);

        for (List<UUID> list : teamAssignments.values()) {
            for (UUID u : list) {
                playerToMatch.put(u, match.getMatchId());
            }
        }
        if (spectators != null) {
            for (UUID u : spectators) {
                spectatorToMatch.put(u, match.getMatchId());
            }
        }

        match.startMatch();
        return match;
    }

    public static void spectateMatch(ServerPlayer player, UUID matchId) {
        ArenaMatch match = activeMatches.get(matchId);
        if (match == null) {
            player.sendSystemMessage(Component.literal("§c[PvpArena] That match is no longer running!"), false);
            return;
        }

        if (isInMatch(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c[PvpArena] You are currently participating in a match!"), false);
            return;
        }

        UUID existingMatch = spectatorToMatch.get(player.getUUID());
        if (existingMatch != null) {
            ArenaMatch oldMatch = activeMatches.get(existingMatch);
            if (oldMatch != null) oldMatch.removeSpectator(player);
        }

        spectatorToMatch.put(player.getUUID(), matchId);
        match.addSpectator(player);
    }

    public static void registerSpectator(UUID uuid, UUID matchId) {
        spectatorToMatch.put(uuid, matchId);
    }

    public static void unregisterSpectator(UUID uuid) {
        spectatorToMatch.remove(uuid);
    }

    public static void tickMatches() {
        for (ArenaMatch match : activeMatches.values()) {
            match.tick();
        }
    }

    public static ArenaMatch getPlayerMatch(UUID uuid) {
        UUID matchId = playerToMatch.get(uuid);
        if (matchId == null) matchId = spectatorToMatch.get(uuid);
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    public static boolean isInMatch(UUID uuid) {
        return playerToMatch.containsKey(uuid);
    }

    public static boolean isSpectating(UUID uuid) {
        return spectatorToMatch.containsKey(uuid);
    }

    public static void removeMatch(UUID matchId) {
        ArenaMatch match = activeMatches.remove(matchId);
        if (match != null) {
            playerToMatch.entrySet().removeIf(e -> e.getValue().equals(matchId));
            spectatorToMatch.entrySet().removeIf(e -> e.getValue().equals(matchId));
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        ArenaMatch match = getPlayerMatch(player.getUUID());
        if (match != null) {
            if (isInMatch(player.getUUID())) {
                match.forfeitPlayer(player);
            } else if (isSpectating(player.getUUID())) {
                match.removeSpectator(player);
            }
        }
    }

    public static Collection<ArenaMatch> getActiveMatches() {
        return Collections.unmodifiableCollection(activeMatches.values());
    }
}