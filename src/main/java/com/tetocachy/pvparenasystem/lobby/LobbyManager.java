package com.tetocachy.pvparenasystem.lobby;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.party.Party;
import com.tetocachy.pvparenasystem.party.PartyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {
    private static final Map<UUID, ArenaLobby> lobbies = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToLobby = new ConcurrentHashMap<>();

    public static ArenaLobby createLobby(ServerPlayer host, Arena arena, Kit kit, int teamCount, int playersPerTeam, int rounds) {
        leaveCurrentLobby(host, host.level().getServer());

        ArenaLobby lobby = new ArenaLobby(host.getUUID(), host.getScoreboardName(), arena, kit, teamCount, playersPerTeam, rounds);
        lobbies.put(lobby.getLobbyId(), lobby);
        playerToLobby.put(host.getUUID(), lobby.getLobbyId());

        MinecraftServer server = host.level().getServer();
        host.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Lobby] Lobby created for §e" + (arena != null ? arena.getDisplayName() : "Arena") + "§a!"), false);

        // Pull Party Members Automatically
        Party party = PartyManager.getParty(host.getUUID());
        if (party != null && party.getLeader().equals(host.getUUID()) && server != null) {
            for (UUID mUuid : party.getMembers()) {
                if (!mUuid.equals(host.getUUID())) {
                    ServerPlayer mp = server.getPlayerList().getPlayer(mUuid);
                    if (mp != null) {
                        leaveCurrentLobby(mp, server);
                        lobby.joinTeam(mUuid, 1);
                        playerToLobby.put(mUuid, lobby.getLobbyId());
                        mp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Lobby] Joined leader's lobby!"), false);
                    }
                }
            }
        }

        return lobby;
    }

    public static void joinLobby(ServerPlayer player, UUID lobbyId, int teamIndex, MinecraftServer server) {
        ArenaLobby lobby = lobbies.get(lobbyId);
        if (lobby != null) {
            leaveCurrentLobby(player, server);
            if (lobby.joinTeam(player.getUUID(), teamIndex)) {
                playerToLobby.put(player.getUUID(), lobbyId);
                lobby.broadcast(server, "§e[Lobby] " + player.getScoreboardName() + " joined Team " + teamIndex + "!");

                // Pull party members
                Party party = PartyManager.getParty(player.getUUID());
                if (party != null && party.getLeader().equals(player.getUUID())) {
                    for (UUID mUuid : party.getMembers()) {
                        if (!mUuid.equals(player.getUUID())) {
                            ServerPlayer mp = server.getPlayerList().getPlayer(mUuid);
                            if (mp != null) {
                                leaveCurrentLobby(mp, server);
                                lobby.joinTeam(mUuid, teamIndex);
                                playerToLobby.put(mUuid, lobbyId);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void switchTeam(ServerPlayer player, int newTeamIndex, MinecraftServer server) {
        ArenaLobby lobby = getPlayerLobby(player.getUUID());
        if (lobby != null) {
            if (lobby.joinTeam(player.getUUID(), newTeamIndex)) {
                lobby.broadcast(server, "§e[Lobby] " + player.getScoreboardName() + " switched to Team " + newTeamIndex + "!");
            }
        }
    }

    public static void leaveCurrentLobby(ServerPlayer player, MinecraftServer server) {
        UUID pUuid = player.getUUID();
        UUID lobbyId = playerToLobby.remove(pUuid);
        if (lobbyId != null) {
            ArenaLobby lobby = lobbies.get(lobbyId);
            if (lobby != null) {
                lobby.leave(pUuid);
                lobby.broadcast(server, "§c[Lobby] " + player.getScoreboardName() + " left the lobby.");
                if (lobby.getHostUuid().equals(pUuid)) {
                    disbandLobby(lobbyId, server);
                }
            }
        }
    }

    public static void disbandLobby(UUID lobbyId, MinecraftServer server) {
        ArenaLobby lobby = lobbies.remove(lobbyId);
        if (lobby != null) {
            if (server != null) lobby.broadcast(server, "§c[Lobby] The lobby was closed by the host.");
            playerToLobby.entrySet().removeIf(e -> e.getValue().equals(lobbyId));
        }
    }

    public static ArenaLobby getPlayerLobby(UUID playerUuid) {
        UUID id = playerToLobby.get(playerUuid);
        return id != null ? lobbies.get(id) : null;
    }

    public static Collection<ArenaLobby> getAllLobbies() {
        return Collections.unmodifiableCollection(lobbies.values());
    }
}