package com.tetocachy.pvparenasystem.match;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.kit.Kit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchManager {
    private static final Map<UUID, ArenaMatch> activeMatches = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToMatch = new ConcurrentHashMap<>();

    public static ArenaMatch createMatch(MinecraftServer server, Arena arena, Kit kit, int rounds, Map<Integer, List<UUID>> teamAssignments) {
        ArenaMatch match = new ArenaMatch(server, arena, kit, rounds, teamAssignments);
        activeMatches.put(match.getMatchId(), match);
        for (List<UUID> list : teamAssignments.values()) {
            for (UUID u : list) {
                playerToMatch.put(u, match.getMatchId());
            }
        }
        match.startMatch();
        return match;
    }

    public static void tickMatches() {
        for (ArenaMatch match : activeMatches.values()) {
            match.tick();
        }
    }

    public static ArenaMatch getPlayerMatch(UUID uuid) {
        UUID matchId = playerToMatch.get(uuid);
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    public static boolean isInMatch(UUID uuid) {
        return playerToMatch.containsKey(uuid);
    }

    public static void removeMatch(UUID matchId) {
        ArenaMatch match = activeMatches.remove(matchId);
        if (match != null) {
            playerToMatch.entrySet().removeIf(e -> e.getValue().equals(matchId));
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        ArenaMatch match = getPlayerMatch(player.getUUID());
        if (match != null) {
            match.forfeitPlayer(player);
        }
    }
}