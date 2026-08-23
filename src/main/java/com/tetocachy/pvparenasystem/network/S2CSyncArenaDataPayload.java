package com.tetocachy.pvparenasystem.network;

import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record S2CSyncArenaDataPayload(
        boolean isAdmin,
        boolean inSetup,
        List<String> onlinePlayers,
        List<KitInfo> kits,
        List<ArenaInfo> arenas,
        PartyInfo party,
        List<DuelInviteInfo> invites
) implements CustomPacketPayload {
    public static final Type<S2CSyncArenaDataPayload> TYPE = new Type<>(PvpArenaSystem.id("s2c_sync_arena_data"));

    public record KitInfo(String id, String displayName) {}
    public record ArenaInfo(String id, String displayName, int status, int teamSpawnCount) {}
    public record PartyInfo(boolean inParty, boolean isLeader, String leaderName, List<String> members) {}
    public record DuelInviteInfo(String senderName, String kitName, String arenaName, int rounds) {}

    public static final StreamCodec<FriendlyByteBuf, S2CSyncArenaDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.isAdmin());
                buf.writeBoolean(p.inSetup());

                // Players
                buf.writeInt(p.onlinePlayers().size());
                for (String player : p.onlinePlayers()) buf.writeUtf(player);

                // Kits
                buf.writeInt(p.kits().size());
                for (KitInfo k : p.kits()) {
                    buf.writeUtf(k.id());
                    buf.writeUtf(k.displayName());
                }

                // Arenas
                buf.writeInt(p.arenas().size());
                for (ArenaInfo a : p.arenas()) {
                    buf.writeUtf(a.id());
                    buf.writeUtf(a.displayName());
                    buf.writeInt(a.status());
                    buf.writeInt(a.teamSpawnCount());
                }

                // Party
                buf.writeBoolean(p.party().inParty());
                buf.writeBoolean(p.party().isLeader());
                buf.writeUtf(p.party().leaderName());
                buf.writeInt(p.party().members().size());
                for (String m : p.party().members()) buf.writeUtf(m);

                // Invites
                buf.writeInt(p.invites().size());
                for (DuelInviteInfo inv : p.invites()) {
                    buf.writeUtf(inv.senderName());
                    buf.writeUtf(inv.kitName());
                    buf.writeUtf(inv.arenaName());
                    buf.writeInt(inv.rounds());
                }
            },
            buf -> {
                boolean isAdmin = buf.readBoolean();
                boolean inSetup = buf.readBoolean();

                int playerCount = buf.readInt();
                List<String> players = new ArrayList<>(playerCount);
                for (int i = 0; i < playerCount; i++) players.add(buf.readUtf());

                int kitCount = buf.readInt();
                List<KitInfo> kits = new ArrayList<>(kitCount);
                for (int i = 0; i < kitCount; i++) kits.add(new KitInfo(buf.readUtf(), buf.readUtf()));

                int arenaCount = buf.readInt();
                List<ArenaInfo> arenas = new ArrayList<>(arenaCount);
                for (int i = 0; i < arenaCount; i++) arenas.add(new ArenaInfo(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt()));

                boolean inParty = buf.readBoolean();
                boolean isLeader = buf.readBoolean();
                String leaderName = buf.readUtf();
                int memberCount = buf.readInt();
                List<String> members = new ArrayList<>(memberCount);
                for (int i = 0; i < memberCount; i++) members.add(buf.readUtf());
                PartyInfo party = new PartyInfo(inParty, isLeader, leaderName, members);

                int inviteCount = buf.readInt();
                List<DuelInviteInfo> invites = new ArrayList<>(inviteCount);
                for (int i = 0; i < inviteCount; i++) invites.add(new DuelInviteInfo(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readInt()));

                return new S2CSyncArenaDataPayload(isAdmin, inSetup, players, kits, arenas, party, invites);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}