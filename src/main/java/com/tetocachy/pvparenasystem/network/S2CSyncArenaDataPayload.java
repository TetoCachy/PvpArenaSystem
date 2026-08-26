package com.tetocachy.pvparenasystem.network;

import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record S2CSyncArenaDataPayload(
        boolean isAdmin,
        boolean inSetup,
        boolean inMatch,
        String editingArenaId,
        BlockPos pos1,
        BlockPos pos2,
        List<String> onlinePlayers,
        List<KitInfo> kits,
        List<ArenaInfo> arenas,
        List<LobbyInfo> lobbies,
        LobbyInfo currentLobby,
        PartyInfo party,
        List<PublicPartyInfo> publicParties,
        List<OngoingMatchInfo> activeMatches,
        List<Integer> allowedTeamCounts,
        List<Integer> allowedTeamSizes,
        List<Integer> allowedGoalPoints
) implements CustomPacketPayload {
    public static final Type<S2CSyncArenaDataPayload> TYPE = new Type<>(PvpArenaSystem.id("s2c_sync_arena_data"));

    public record SpawnPointData(int teamIndex, double x, double y, double z) {}
    public record KitInfo(String id, String displayName, List<ItemStack> previewItems) {}
    public record ArenaInfo(
            String id,
            String displayName,
            int status,
            int activeFights,
            int maxTeams,
            int maxPlayersPerTeam,
            int teamSpawnCount,
            boolean hasSpectatorSpawn,
            int sizeX,
            int sizeY,
            int sizeZ,
            String borderShape,
            List<SpawnPointData> spawns
    ) {}
    public record LobbyInfo(String lobbyId, String hostName, String arenaName, String kitName, int teamCount, int playersPerTeam, int pointsToWin, boolean friendlyFire, List<TeamSlotInfo> teams, List<String> spectatorNames) {}
    public record TeamSlotInfo(int teamIndex, List<String> memberNames) {}
    public record PartyInfo(boolean inParty, boolean isLeader, boolean isPublic, String partyName, String leaderName, int maxMembers, List<String> members) {}
    public record PublicPartyInfo(String partyName, String leaderName, int memberCount, int maxMembers) {}
    public record OngoingMatchInfo(String matchId, String arenaName, String kitName, int currentRound, int pointsToWin, boolean friendlyFire, int playerCount, int spectatorCount) {}

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSyncArenaDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.isAdmin());
                buf.writeBoolean(p.inSetup());
                buf.writeBoolean(p.inMatch());
                buf.writeUtf(p.editingArenaId() != null ? p.editingArenaId() : "");

                buf.writeBoolean(p.pos1() != null);
                if (p.pos1() != null) buf.writeBlockPos(p.pos1());
                buf.writeBoolean(p.pos2() != null);
                if (p.pos2() != null) buf.writeBlockPos(p.pos2());

                buf.writeInt(p.onlinePlayers().size());
                for (String pl : p.onlinePlayers()) buf.writeUtf(pl);

                buf.writeInt(p.kits().size());
                for (KitInfo k : p.kits()) {
                    buf.writeUtf(k.id());
                    buf.writeUtf(k.displayName());
                    buf.writeInt(k.previewItems().size());
                    for (ItemStack stack : k.previewItems()) {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                    }
                }

                buf.writeInt(p.arenas().size());
                for (ArenaInfo a : p.arenas()) {
                    buf.writeUtf(a.id());
                    buf.writeUtf(a.displayName());
                    buf.writeInt(a.status());
                    buf.writeInt(a.activeFights());
                    buf.writeInt(a.maxTeams());
                    buf.writeInt(a.maxPlayersPerTeam());
                    buf.writeInt(a.teamSpawnCount());
                    buf.writeBoolean(a.hasSpectatorSpawn());
                    buf.writeInt(a.sizeX());
                    buf.writeInt(a.sizeY());
                    buf.writeInt(a.sizeZ());
                    buf.writeUtf(a.borderShape() != null ? a.borderShape() : "BOX");
                    buf.writeInt(a.spawns().size());
                    for (SpawnPointData sp : a.spawns()) {
                        buf.writeInt(sp.teamIndex());
                        buf.writeDouble(sp.x());
                        buf.writeDouble(sp.y());
                        buf.writeDouble(sp.z());
                    }
                }

                buf.writeInt(p.lobbies().size());
                for (LobbyInfo l : p.lobbies()) writeLobby(buf, l);

                buf.writeBoolean(p.currentLobby() != null);
                if (p.currentLobby() != null) writeLobby(buf, p.currentLobby());

                buf.writeBoolean(p.party().inParty());
                buf.writeBoolean(p.party().isLeader());
                buf.writeBoolean(p.party().isPublic());
                buf.writeUtf(p.party().partyName());
                buf.writeUtf(p.party().leaderName());
                buf.writeInt(p.party().maxMembers());
                buf.writeInt(p.party().members().size());
                for (String m : p.party().members()) buf.writeUtf(m);

                buf.writeInt(p.publicParties().size());
                for (PublicPartyInfo pub : p.publicParties()) {
                    buf.writeUtf(pub.partyName());
                    buf.writeUtf(pub.leaderName());
                    buf.writeInt(pub.memberCount());
                    buf.writeInt(pub.maxMembers());
                }

                buf.writeInt(p.activeMatches().size());
                for (OngoingMatchInfo m : p.activeMatches()) {
                    buf.writeUtf(m.matchId());
                    buf.writeUtf(m.arenaName());
                    buf.writeUtf(m.kitName());
                    buf.writeInt(m.currentRound());
                    buf.writeInt(m.pointsToWin());
                    buf.writeBoolean(m.friendlyFire());
                    buf.writeInt(m.playerCount());
                    buf.writeInt(m.spectatorCount());
                }

                buf.writeInt(p.allowedTeamCounts().size());
                for (int tc : p.allowedTeamCounts()) buf.writeInt(tc);

                buf.writeInt(p.allowedTeamSizes().size());
                for (int ts : p.allowedTeamSizes()) buf.writeInt(ts);

                buf.writeInt(p.allowedGoalPoints().size());
                for (int gp : p.allowedGoalPoints()) buf.writeInt(gp);
            },
            buf -> {
                boolean isAdmin = buf.readBoolean();
                boolean inSetup = buf.readBoolean();
                boolean inMatch = buf.readBoolean();
                String editingArenaId = buf.readUtf();

                BlockPos p1 = buf.readBoolean() ? buf.readBlockPos() : null;
                BlockPos p2 = buf.readBoolean() ? buf.readBlockPos() : null;

                int pCount = buf.readInt();
                List<String> players = new ArrayList<>(pCount);
                for (int i = 0; i < pCount; i++) players.add(buf.readUtf());

                int kCount = buf.readInt();
                List<KitInfo> kits = new ArrayList<>(kCount);
                for (int i = 0; i < kCount; i++) {
                    String id = buf.readUtf();
                    String name = buf.readUtf();
                    int itemCount = buf.readInt();
                    List<ItemStack> items = new ArrayList<>(itemCount);
                    for (int j = 0; j < itemCount; j++) {
                        items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    }
                    kits.add(new KitInfo(id, name, items));
                }

                int aCount = buf.readInt();
                List<ArenaInfo> arenas = new ArrayList<>(aCount);
                for (int i = 0; i < aCount; i++) {
                    String aId = buf.readUtf();
                    String aName = buf.readUtf();
                    int status = buf.readInt();
                    int activeFights = buf.readInt();
                    int maxTeams = buf.readInt();
                    int maxPpt = buf.readInt();
                    int teamSpawns = buf.readInt();
                    boolean hasSpec = buf.readBoolean();
                    int sx = buf.readInt();
                    int sy = buf.readInt();
                    int sz = buf.readInt();
                    String borderShape = buf.readUtf();

                    int spawnCount = buf.readInt();
                    List<SpawnPointData> spawns = new ArrayList<>(spawnCount);
                    for (int j = 0; j < spawnCount; j++) {
                        spawns.add(new SpawnPointData(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble()));
                    }

                    arenas.add(new ArenaInfo(aId, aName, status, activeFights, maxTeams, maxPpt, teamSpawns, hasSpec, sx, sy, sz, borderShape, spawns));
                }

                int lCount = buf.readInt();
                List<LobbyInfo> lobbies = new ArrayList<>(lCount);
                for (int i = 0; i < lCount; i++) lobbies.add(readLobby(buf));

                LobbyInfo currentLobby = buf.readBoolean() ? readLobby(buf) : null;

                boolean inParty = buf.readBoolean();
                boolean isLeader = buf.readBoolean();
                boolean isPublic = buf.readBoolean();
                String partyName = buf.readUtf();
                String leaderName = buf.readUtf();
                int maxMembers = buf.readInt();
                int mCount = buf.readInt();
                List<String> partyMembers = new ArrayList<>(mCount);
                for (int i = 0; i < mCount; i++) partyMembers.add(buf.readUtf());
                PartyInfo party = new PartyInfo(inParty, isLeader, isPublic, partyName, leaderName, maxMembers, partyMembers);

                int pubCount = buf.readInt();
                List<PublicPartyInfo> pubParties = new ArrayList<>(pubCount);
                for (int i = 0; i < pubCount; i++) {
                    pubParties.add(new PublicPartyInfo(buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt()));
                }

                int mSize = buf.readInt();
                List<OngoingMatchInfo> activeMatches = new ArrayList<>(mSize);
                for (int i = 0; i < mSize; i++) {
                    activeMatches.add(new OngoingMatchInfo(
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readInt(),
                            buf.readInt()
                    ));
                }

                int tcSize = buf.readInt();
                List<Integer> tc = new ArrayList<>(tcSize);
                for (int i = 0; i < tcSize; i++) tc.add(buf.readInt());

                int tsSize = buf.readInt();
                List<Integer> ts = new ArrayList<>(tsSize);
                for (int i = 0; i < tsSize; i++) ts.add(buf.readInt());

                int gpSize = buf.readInt();
                List<Integer> gp = new ArrayList<>(gpSize);
                for (int i = 0; i < gpSize; i++) gp.add(buf.readInt());

                return new S2CSyncArenaDataPayload(isAdmin, inSetup, inMatch, editingArenaId, p1, p2, players, kits, arenas, lobbies, currentLobby, party, pubParties, activeMatches, tc, ts, gp);
            }
    );

    private static void writeLobby(RegistryFriendlyByteBuf buf, LobbyInfo l) {
        buf.writeUtf(l.lobbyId());
        buf.writeUtf(l.hostName());
        buf.writeUtf(l.arenaName());
        buf.writeUtf(l.kitName());
        buf.writeInt(l.teamCount());
        buf.writeInt(l.playersPerTeam());
        buf.writeInt(l.pointsToWin());
        buf.writeBoolean(l.friendlyFire());
        buf.writeInt(l.teams().size());
        for (TeamSlotInfo t : l.teams()) {
            buf.writeInt(t.teamIndex());
            buf.writeInt(t.memberNames().size());
            for (String name : t.memberNames()) buf.writeUtf(name);
        }
        buf.writeInt(l.spectatorNames().size());
        for (String s : l.spectatorNames()) buf.writeUtf(s);
    }

    private static LobbyInfo readLobby(RegistryFriendlyByteBuf buf) {
        String id = buf.readUtf();
        String host = buf.readUtf();
        String arena = buf.readUtf();
        String kit = buf.readUtf();
        int tc = buf.readInt();
        int ppt = buf.readInt();
        int ptw = buf.readInt();
        boolean ff = buf.readBoolean();
        int tSize = buf.readInt();
        List<TeamSlotInfo> teams = new ArrayList<>(tSize);
        for (int i = 0; i < tSize; i++) {
            int tIndex = buf.readInt();
            int mCount = buf.readInt();
            List<String> members = new ArrayList<>(mCount);
            for (int j = 0; j < mCount; j++) members.add(buf.readUtf());
            teams.add(new TeamSlotInfo(tIndex, members));
        }
        int sSize = buf.readInt();
        List<String> spectators = new ArrayList<>(sSize);
        for (int i = 0; i < sSize; i++) spectators.add(buf.readUtf());
        return new LobbyInfo(id, host, arena, kit, tc, ppt, ptw, ff, teams, spectators);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}