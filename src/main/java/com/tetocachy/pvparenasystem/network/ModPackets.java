package com.tetocachy.pvparenasystem.network;

import com.tetocachy.pvparenasystem.admin.SelectionManager;
import com.tetocachy.pvparenasystem.admin.SetupSession;
import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.arena.SpawnPoint;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.kit.KitManager;
import com.tetocachy.pvparenasystem.lobby.ArenaLobby;
import com.tetocachy.pvparenasystem.lobby.LobbyManager;
import com.tetocachy.pvparenasystem.match.ArenaMatch;
import com.tetocachy.pvparenasystem.match.MatchManager;
import com.tetocachy.pvparenasystem.match.TeamData;
import com.tetocachy.pvparenasystem.party.Party;
import com.tetocachy.pvparenasystem.party.PartyManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class ModPackets {

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(C2SActionPayload.TYPE, C2SActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(S2CSyncArenaDataPayload.TYPE, S2CSyncArenaDataPayload.STREAM_CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(C2SActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = player.level().getServer();
            if (server == null) return;
            context.server().execute(() -> handleAction(player, server, payload));
        });
    }

    public static void broadcastSync(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSyncToPlayer(player);
        }
    }

    public static void sendSyncToPlayer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        boolean isAdmin = server.getPlayerList().isOp(player.nameAndId());
        boolean inSetup = SetupSession.isInSetup(player.getUUID());
        Arena editingArena = SetupSession.getCurrentEditingArena(player.getUUID());

        List<String> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.getUUID().equals(player.getUUID())) players.add(p.getScoreboardName());
        }

        List<S2CSyncArenaDataPayload.KitInfo> kits = new ArrayList<>();
        for (Kit k : KitManager.getAllKits()) {
            List<ItemStack> previewStacks = k.getItems(player.registryAccess());
            kits.add(new S2CSyncArenaDataPayload.KitInfo(k.getId(), k.getDisplayName(), previewStacks));
        }

        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = new ArrayList<>();
        for (Arena a : ArenaManager.getAllArenas()) {
            int status = a.isInUse() ? 2 : (a.isConfigured() ? 1 : 0);
            arenas.add(new S2CSyncArenaDataPayload.ArenaInfo(a.getId(), a.getDisplayName(), status, a.getMaxTeams(), a.getMaxPlayersPerTeam(), a.getAllTeamSpawns().size()));
        }

        List<S2CSyncArenaDataPayload.LobbyInfo> lobbies = new ArrayList<>();
        for (ArenaLobby l : LobbyManager.getAllLobbies()) {
            lobbies.add(toLobbyInfo(server, l));
        }

        ArenaLobby curLobby = LobbyManager.getPlayerLobby(player.getUUID());
        S2CSyncArenaDataPayload.LobbyInfo currentLobbyInfo = curLobby != null ? toLobbyInfo(server, curLobby) : null;

        Party party = PartyManager.getParty(player.getUUID());
        S2CSyncArenaDataPayload.PartyInfo partyInfo;
        if (party != null) {
            boolean isLeader = party.getLeader().equals(player.getUUID());
            ServerPlayer leaderPlayer = server.getPlayerList().getPlayer(party.getLeader());
            String leaderName = leaderPlayer != null ? leaderPlayer.getScoreboardName() : "Unknown";
            List<String> memberNames = new ArrayList<>();
            for (UUID mUuid : party.getMembers()) {
                ServerPlayer mp = server.getPlayerList().getPlayer(mUuid);
                if (mp != null) memberNames.add(mp.getScoreboardName());
            }
            partyInfo = new S2CSyncArenaDataPayload.PartyInfo(true, isLeader, party.isPublic(), party.getName(), leaderName, party.getMaxMembers(), memberNames);
        } else {
            partyInfo = new S2CSyncArenaDataPayload.PartyInfo(false, false, false, "", "", 8, List.of());
        }

        List<S2CSyncArenaDataPayload.PublicPartyInfo> pubParties = new ArrayList<>();
        for (Party p : PartyManager.getAllParties()) {
            if (p.isPublic()) {
                ServerPlayer leader = server.getPlayerList().getPlayer(p.getLeader());
                String lName = leader != null ? leader.getScoreboardName() : "Unknown";
                pubParties.add(new S2CSyncArenaDataPayload.PublicPartyInfo(p.getName(), lName, p.getMembers().size(), p.getMaxMembers()));
            }
        }

        List<S2CSyncArenaDataPayload.OngoingMatchInfo> activeMatches = new ArrayList<>();
        for (ArenaMatch m : MatchManager.getActiveMatches()) {
            int totalP = 0;
            for (TeamData td : m.getTeams().values()) {
                totalP += td.getMembers().size();
            }
            activeMatches.add(new S2CSyncArenaDataPayload.OngoingMatchInfo(
                    m.getMatchId().toString(),
                    m.getArena().getDisplayName(),
                    m.getKit() != null ? m.getKit().getDisplayName() : "Default Kit",
                    m.getCurrentRound(),
                    m.getRoundsToWin() * 2 - 1,
                    totalP,
                    m.getSpectators().size()
            ));
        }

        S2CSyncArenaDataPayload sync = new S2CSyncArenaDataPayload(
                isAdmin, inSetup, editingArena != null ? editingArena.getId() : "",
                SelectionManager.getPos1(player.getUUID()), SelectionManager.getPos2(player.getUUID()),
                players, kits, arenas, lobbies, currentLobbyInfo, partyInfo, pubParties, activeMatches
        );

        ServerPlayNetworking.send(player, sync);
    }

    private static S2CSyncArenaDataPayload.LobbyInfo toLobbyInfo(MinecraftServer server, ArenaLobby l) {
        List<S2CSyncArenaDataPayload.TeamSlotInfo> teams = new ArrayList<>();
        for (int i = 1; i <= l.getTeamCount(); i++) {
            List<UUID> slotList = l.getTeamSlots().getOrDefault(i, Collections.emptyList());
            List<String> names = new ArrayList<>();
            for (UUID u : slotList) {
                ServerPlayer p = server.getPlayerList().getPlayer(u);
                if (p != null) names.add(p.getScoreboardName());
            }
            teams.add(new S2CSyncArenaDataPayload.TeamSlotInfo(i, names));
        }

        List<String> specNames = new ArrayList<>();
        for (UUID u : l.getSpectators()) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) specNames.add(p.getScoreboardName());
        }

        return new S2CSyncArenaDataPayload.LobbyInfo(
                l.getLobbyId().toString(), l.getHostName(),
                l.getArena() != null ? l.getArena().getDisplayName() : "Any Arena",
                l.getKit() != null ? l.getKit().getDisplayName() : "Default Kit",
                l.getTeamCount(), l.getPlayersPerTeam(), l.getRounds(), teams, specNames
        );
    }

    private static void handleAction(ServerPlayer player, MinecraftServer server, C2SActionPayload payload) {
        boolean isAdmin = server.getPlayerList().isOp(player.nameAndId());
        String action = payload.action();

        switch (action) {
            case "REQUEST_SYNC" -> {}

            case "LOBBY_CREATE" -> {
                Arena arena = ArenaManager.getAvailableArena(payload.param1());
                Kit kit = KitManager.getKit(payload.param2());
                int teams = Math.max(2, payload.intParam1());
                int playersPerTeam = Math.max(1, payload.intParam2());
                int rounds = Math.max(1, payload.intParam3() > 0 ? payload.intParam3() : 3);
                LobbyManager.createLobby(player, arena, kit, teams, playersPerTeam, rounds);
            }
            case "LOBBY_JOIN" -> {
                try {
                    UUID lId = UUID.fromString(payload.param1());
                    LobbyManager.joinLobby(player, lId, Math.max(0, payload.intParam1()), server);
                } catch (Exception ignored) {}
            }
            case "LOBBY_JOIN_SPECTATOR" -> {
                try {
                    UUID lId = UUID.fromString(payload.param1());
                    LobbyManager.joinAsSpectator(player, lId, server);
                } catch (Exception ignored) {}
            }
            case "LOBBY_BECOME_SPECTATOR" -> LobbyManager.becomeSpectator(player, server);
            case "LOBBY_SWITCH_TEAM" -> LobbyManager.switchTeam(player, payload.intParam1(), server);
            case "LOBBY_LEAVE" -> LobbyManager.leaveCurrentLobby(player, server);
            case "LOBBY_START" -> {
                ArenaLobby lobby = LobbyManager.getPlayerLobby(player.getUUID());
                if (lobby != null && lobby.getHostUuid().equals(player.getUUID())) {
                    if (lobby.canStart()) {
                        lobby.startMatch(server);
                        LobbyManager.disbandLobby(lobby.getLobbyId(), server);
                    } else {
                        player.sendSystemMessage(Component.literal("§c[Lobby] At least 2 teams must have players to start!"), false);
                    }
                }
            }

            case "MATCH_SPECTATE" -> {
                try {
                    UUID mId = UUID.fromString(payload.param1());
                    MatchManager.spectateMatch(player, mId);
                } catch (Exception ignored) {}
            }

            case "PARTY_CREATE" -> PartyManager.createParty(player);
            case "PARTY_RENAME" -> {
                Party p = PartyManager.getParty(player.getUUID());
                if (p != null && p.getLeader().equals(player.getUUID()) && !payload.param1().isBlank()) {
                    p.setName(payload.param1().trim());
                    p.broadcast(server, "§e[Party] Renamed party to '§f" + p.getName() + "§e'!");
                }
            }
            case "PARTY_SET_MAX_MEMBERS" -> {
                Party p = PartyManager.getParty(player.getUUID());
                if (p != null && p.getLeader().equals(player.getUUID())) {
                    p.setMaxMembers(payload.intParam1());
                    p.broadcast(server, "§e[Party] Max party members set to §b" + p.getMaxMembers() + "§e.");
                }
            }
            case "PARTY_TOGGLE_PUBLIC" -> {
                Party p = PartyManager.getParty(player.getUUID());
                if (p != null && p.getLeader().equals(player.getUUID())) {
                    p.setPublic(!p.isPublic());
                    p.broadcast(server, "§e[Party] Party is now " + (p.isPublic() ? "§aPUBLIC" : "§cPRIVATE") + "§e.");
                }
            }
            case "PARTY_JOIN_PUBLIC" -> {
                for (Party p : PartyManager.getAllParties()) {
                    if (p.getName().equalsIgnoreCase(payload.param1()) && p.isPublic()) {
                        PartyManager.joinParty(player, p, server);
                        break;
                    }
                }
            }
            case "PARTY_LEAVE" -> PartyManager.leaveParty(player, server);

            case "SET_SPAWN_AT_BLOCK" -> {
                if (isAdmin && SetupSession.isInSetup(player.getUUID())) {
                    Arena a = SetupSession.getCurrentEditingArena(player.getUUID());
                    if (a != null) {
                        int type = payload.intParam1();
                        SpawnPoint sp = SpawnPoint.fromPlayer(player);
                        if (type == 99) {
                            a.setSpectatorSpawn(sp);
                            player.sendSystemMessage(Component.literal("§aSpectator spawn set!"), false);
                        } else if (type == 100) {
                            a.setLobbySpawn(sp);
                            player.sendSystemMessage(Component.literal("§aLobby spawn set!"), false);
                        } else {
                            a.addTeamSpawn(type, sp);
                            player.sendSystemMessage(Component.literal("§aSpawn for Team " + type + " added!"), false);
                        }
                    }
                }
            }
            case "ADMIN_WAND" -> {
                if (isAdmin) SelectionManager.toggleWand(player);
            }
            case "ADMIN_CREATE_ARENA" -> {
                if (isAdmin) {
                    BlockPos p1 = SelectionManager.getPos1(player.getUUID());
                    BlockPos p2 = SelectionManager.getPos2(player.getUUID());
                    if (p1 != null && p2 != null) {
                        Arena arena = ArenaManager.createArenaFromSelection(server, payload.param1(), payload.param1(), (ServerLevel) player.level(), p1, p2);
                        SetupSession.startSetup(player, arena);
                    }
                }
            }
            case "ADMIN_SAVE_ARENA" -> {
                if (isAdmin && SetupSession.isInSetup(player.getUUID())) {
                    Arena a = SetupSession.getCurrentEditingArena(player.getUUID());
                    if (a != null) {
                        a.captureMapSnapshot(ModDimensions.getArenaLevel(server));
                        ArenaManager.saveArena(server, a);
                        player.sendSystemMessage(Component.literal("§aArena saved!"), false);
                    }
                }
            }
            case "ADMIN_LEAVE_SETUP" -> {
                if (SetupSession.isInSetup(player.getUUID())) SetupSession.finishSetup(player);
            }
            case "ADMIN_SAVE_KIT" -> {
                if (isAdmin) {
                    Kit kit = Kit.fromPlayer(payload.param1(), payload.param1(), player);
                    KitManager.saveKit(server, kit);
                    player.sendSystemMessage(Component.literal("§aKit " + payload.param1() + " saved!"), false);
                }
            }
        }

        broadcastSync(server);
    }
}