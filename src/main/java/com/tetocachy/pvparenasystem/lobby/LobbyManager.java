package com.tetocachy.pvparenasystem.lobby;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.party.Party;
import com.tetocachy.pvparenasystem.party.PartyManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager {
    private static final Map<UUID, ArenaLobby> lobbies = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToLobby = new ConcurrentHashMap<>();

    public static ArenaLobby createLobby(ServerPlayer host, Arena arena, Kit kit, int teamCount, int playersPerTeam, int pointsToWin, boolean friendlyFire) {
        Party party = PartyManager.getParty(host.getUUID());
        if (party != null && !party.getLeader().equals(host.getUUID())) {
            host.sendSystemMessage(Component.literal("§c[Party] Only the party leader can create a lobby!"), false);
            return null;
        }

        leaveCurrentLobby(host, host.level().getServer());

        ArenaLobby lobby = new ArenaLobby(host.getUUID(), host.getScoreboardName(), arena, kit, teamCount, playersPerTeam, pointsToWin, friendlyFire);
        lobbies.put(lobby.getLobbyId(), lobby);
        playerToLobby.put(host.getUUID(), lobby.getLobbyId());

        MinecraftServer server = host.level().getServer();
        host.sendSystemMessage(Component.literal("§a[Lobby] Lobby created for §e" + (arena != null ? arena.getDisplayName() : "Arena") + "§a!"), false);

        if (party != null && party.getLeader().equals(host.getUUID()) && server != null) {
            for (UUID mUuid : party.getMembers()) {
                if (!mUuid.equals(host.getUUID())) {
                    ServerPlayer mp = server.getPlayerList().getPlayer(mUuid);
                    if (mp != null) {
                        leaveCurrentLobby(mp, server);
                        int targetTeam = lobby.findAvailableTeam();
                        if (targetTeam > 0) {
                            lobby.joinTeam(mUuid, targetTeam);
                        } else {
                            lobby.joinSpectator(mUuid);
                        }
                        playerToLobby.put(mUuid, lobby.getLobbyId());
                        mp.sendSystemMessage(Component.literal("§a[Lobby] Joined leader's lobby!"), false);
                    }
                }
            }
        }

        return lobby;
    }

    public static boolean joinLobby(ServerPlayer player, UUID lobbyId, int preferredTeam, MinecraftServer server) {
        Party party = PartyManager.getParty(player.getUUID());
        if (party != null && !party.getLeader().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c[Party] Only the party leader can join a lobby!"), false);
            return false;
        }

        ArenaLobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            player.sendSystemMessage(Component.literal("§c[Lobby] Lobby no longer exists!"), false);
            return false;
        }

        int targetTeam = preferredTeam;
        List<UUID> slot = lobby.getTeamSlots().get(targetTeam);
        if (slot == null || slot.size() >= lobby.getPlayersPerTeam()) {
            targetTeam = lobby.findAvailableTeam();
        }

        if (targetTeam <= 0) {
            player.sendSystemMessage(Component.literal("§c[Lobby] This lobby is full! You can join as a spectator instead."), false);
            return false;
        }

        leaveCurrentLobby(player, server);
        if (lobby.joinTeam(player.getUUID(), targetTeam)) {
            playerToLobby.put(player.getUUID(), lobbyId);
            lobby.broadcast(server, "§e[Lobby] " + player.getScoreboardName() + " joined Team " + targetTeam + "!");

            if (party != null && party.getLeader().equals(player.getUUID())) {
                for (UUID mUuid : party.getMembers()) {
                    if (!mUuid.equals(player.getUUID())) {
                        ServerPlayer mp = server.getPlayerList().getPlayer(mUuid);
                        if (mp != null) {
                            int memberTeam = lobby.findAvailableTeam();
                            leaveCurrentLobby(mp, server);
                            if (memberTeam > 0) {
                                lobby.joinTeam(mUuid, memberTeam);
                            } else {
                                lobby.joinSpectator(mUuid);
                            }
                            playerToLobby.put(mUuid, lobbyId);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static void joinAsSpectator(ServerPlayer player, UUID lobbyId, MinecraftServer server) {
        ArenaLobby lobby = lobbies.get(lobbyId);
        if (lobby == null) {
            player.sendSystemMessage(Component.literal("§c[Lobby] Lobby no longer exists!"), false);
            return;
        }

        leaveCurrentLobby(player, server);
        lobby.joinSpectator(player.getUUID());
        playerToLobby.put(player.getUUID(), lobbyId);
        lobby.broadcast(server, "§7[Lobby] " + player.getScoreboardName() + " joined as a spectator.");
        player.sendSystemMessage(Component.literal("§a[Lobby] You are now spectating this lobby."), false);
    }

    public static void becomeSpectator(ServerPlayer player, MinecraftServer server) {
        ArenaLobby lobby = getPlayerLobby(player.getUUID());
        if (lobby != null) {
            lobby.joinSpectator(player.getUUID());
            lobby.broadcast(server, "§7[Lobby] " + player.getScoreboardName() + " is now a spectator.");
            player.sendSystemMessage(Component.literal("§e[Lobby] You switched to spectator mode."), false);
        }
    }

    public static void switchTeam(ServerPlayer player, int newTeamIndex, MinecraftServer server) {
        ArenaLobby lobby = getPlayerLobby(player.getUUID());
        if (lobby != null) {
            if (lobby.joinTeam(player.getUUID(), newTeamIndex)) {
                lobby.broadcast(server, "§e[Lobby] " + player.getScoreboardName() + " switched to Team " + newTeamIndex + "!");
            } else {
                player.sendSystemMessage(Component.literal("§c[Lobby] Team " + newTeamIndex + " is full!"), false);
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