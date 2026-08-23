package com.tetocachy.pvparenasystem.party;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    private static final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerPartyMap = new ConcurrentHashMap<>();

    public static Party createParty(UUID leader) {
        Party party = new Party(leader);
        parties.put(leader, party);
        playerPartyMap.put(leader, leader);
        return party;
    }

    public static Party getParty(UUID uuid) {
        UUID leader = playerPartyMap.get(uuid);
        return leader != null ? parties.get(leader) : null;
    }

    public static void disband(UUID leader) {
        Party party = parties.remove(leader);
        if (party != null) {
            for (UUID m : party.getMembers()) {
                playerPartyMap.remove(m);
            }
        }
    }
}