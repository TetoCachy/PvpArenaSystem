package com.tetocachy.pvparenasystem.network;

import com.tetocachy.pvparenasystem.admin.SelectionManager;
import com.tetocachy.pvparenasystem.admin.SetupSession;
import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.arena.SpawnPoint;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import com.tetocachy.pvparenasystem.duel.DuelManager;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.kit.KitManager;
import com.tetocachy.pvparenasystem.match.MatchManager;
import com.tetocachy.pvparenasystem.party.Party;
import com.tetocachy.pvparenasystem.party.PartyManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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

    public static void sendSyncToPlayer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        boolean isAdmin = server.getPlayerList().isOp(player.nameAndId());
        boolean inSetup = SetupSession.isInSetup(player.getUUID());

        List<String> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.getUUID().equals(player.getUUID())) {
                players.add(p.getScoreboardName());
            }
        }

        List<S2CSyncArenaDataPayload.KitInfo> kits = new ArrayList<>();
        for (Kit k : KitManager.getAllKits()) {
            kits.add(new S2CSyncArenaDataPayload.KitInfo(k.getId(), k.getDisplayName()));
        }

        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = new ArrayList<>();
        for (Arena a : ArenaManager.getAllArenas()) {
            int status = a.isInUse() ? 2 : (a.isConfigured() ? 1 : 0);
            arenas.add(new S2CSyncArenaDataPayload.ArenaInfo(a.getId(), a.getDisplayName(), status, a.getAllTeamSpawns().size()));
        }

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
            partyInfo = new S2CSyncArenaDataPayload.PartyInfo(true, isLeader, leaderName, memberNames);
        } else {
            partyInfo = new S2CSyncArenaDataPayload.PartyInfo(false, false, "", List.of());
        }

        S2CSyncArenaDataPayload sync = new S2CSyncArenaDataPayload(isAdmin, inSetup, players, kits, arenas, partyInfo, List.of());
        ServerPlayNetworking.send(player, sync);
    }

    private static void handleAction(ServerPlayer player, MinecraftServer server, C2SActionPayload payload) {
        boolean isAdmin = server.getPlayerList().isOp(player.nameAndId());
        String action = payload.action();

        switch (action) {
            case "REQUEST_SYNC" -> sendSyncToPlayer(player);

            case "DUEL_SEND" -> {
                ServerPlayer target = server.getPlayerList().getPlayerByName(payload.param1());
                if (target != null) {
                    DuelManager.sendChallenge(player, target, payload.param2().isEmpty() ? null : payload.param2(), null, Math.max(1, payload.intParam1()));
                } else {
                    player.sendSystemMessage(Component.literal("§cTarget player is offline!"), false);
                }
            }
            case "DUEL_ACCEPT" -> DuelManager.acceptChallenge(player, payload.param1());
            case "DUEL_DECLINE" -> DuelManager.declineChallenge(player, payload.param1());

            case "PARTY_CREATE" -> PartyManager.createParty(player.getUUID());
            case "PARTY_INVITE" -> {
                ServerPlayer target = server.getPlayerList().getPlayerByName(payload.param1());
                Party party = PartyManager.getParty(player.getUUID());
                if (target != null && party != null && party.getLeader().equals(player.getUUID())) {
                    party.addMember(target.getUUID());
                    target.sendSystemMessage(Component.literal("§aYou joined §e" + player.getScoreboardName() + "'s §aparty!"), false);
                }
            }
            case "PARTY_LEAVE" -> {
                Party party = PartyManager.getParty(player.getUUID());
                if (party != null) {
                    if (party.getLeader().equals(player.getUUID())) {
                        PartyManager.disband(player.getUUID());
                    } else {
                        party.removeMember(player.getUUID());
                    }
                }
            }

            // Admin Actions
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
                    } else {
                        player.sendSystemMessage(Component.literal("§cSelect Pos 1 and Pos 2 with wand first!"), false);
                    }
                }
            }
            case "ADMIN_SET_SPAWN" -> {
                if (isAdmin && SetupSession.isInSetup(player.getUUID())) {
                    Arena a = SetupSession.getCurrentEditingArena(player.getUUID());
                    if (a != null) {
                        a.addTeamSpawn(payload.intParam1(), SpawnPoint.fromPlayer(player));
                        player.sendSystemMessage(Component.literal("§aSpawn for Team " + payload.intParam1() + " set!"), false);
                    }
                }
            }
            case "ADMIN_SET_SPEC" -> {
                if (isAdmin && SetupSession.isInSetup(player.getUUID())) {
                    Arena a = SetupSession.getCurrentEditingArena(player.getUUID());
                    if (a != null) {
                        a.setSpectatorSpawn(SpawnPoint.fromPlayer(player));
                        player.sendSystemMessage(Component.literal("§aSpectator spawn set!"), false);
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
                if (SetupSession.isInSetup(player.getUUID())) {
                    SetupSession.finishSetup(player);
                }
            }
            case "ADMIN_SAVE_KIT" -> {
                if (isAdmin) {
                    Kit kit = Kit.fromPlayer(payload.param1(), payload.param1(), player);
                    KitManager.saveKit(server, kit);
                    player.sendSystemMessage(Component.literal("§aKit " + payload.param1() + " saved!"), false);
                }
            }
            case "ADMIN_DELETE_KIT" -> {
                if (isAdmin) KitManager.deleteKit(server, payload.param1());
            }
            case "ADMIN_DELETE_ARENA" -> {
                if (isAdmin) ArenaManager.deleteArena(server, payload.param1());
            }
        }

        // Always re-sync after action
        sendSyncToPlayer(player);
    }
}