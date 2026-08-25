package com.tetocachy.pvparenasystem.party;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    private static final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerPartyMap = new ConcurrentHashMap<>();

    public static Party createParty(ServerPlayer leader) {
        disband(leader.getUUID(), leader.level().getServer());

        Party party = new Party(leader.getUUID(), leader.getScoreboardName());
        parties.put(leader.getUUID(), party);
        playerPartyMap.put(leader.getUUID(), leader.getUUID());
        leader.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Party] Created party '§e" + party.getName() + "§a'!"), false);
        return party;
    }

    public static Party getParty(UUID uuid) {
        UUID leader = playerPartyMap.get(uuid);
        return leader != null ? parties.get(leader) : null;
    }

    public static boolean joinParty(ServerPlayer player, Party party, MinecraftServer server) {
        leaveParty(player, server);
        if (party.addMember(player.getUUID())) {
            playerPartyMap.put(player.getUUID(), party.getLeader());
            party.broadcast(server, "§e[Party] " + player.getScoreboardName() + " joined the party! (" + party.getMembers().size() + "/" + party.getMaxMembers() + ")");
            return true;
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Party] Party is full!"), false);
            return false;
        }
    }

    public static void leaveParty(ServerPlayer player, MinecraftServer server) {
        UUID pUuid = player.getUUID();
        UUID leader = playerPartyMap.remove(pUuid);
        if (leader != null) {
            Party party = parties.get(leader);
            if (party != null) {
                if (party.getLeader().equals(pUuid)) {
                    disband(leader, server);
                } else {
                    party.removeMember(pUuid);
                    party.broadcast(server, "§c[Party] " + player.getScoreboardName() + " left the party.");
                }
            }
        }
    }

    public static void disband(UUID leader, MinecraftServer server) {
        Party party = parties.remove(leader);
        if (party != null && server != null) {
            party.broadcast(server, "§c[Party] The party was disbanded.");
            for (UUID m : party.getMembers()) {
                playerPartyMap.remove(m);
            }
        }
    }

    public static Collection<Party> getAllParties() {
        return Collections.unmodifiableCollection(parties.values());
    }
}