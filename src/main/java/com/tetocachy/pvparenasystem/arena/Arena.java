package com.tetocachy.pvparenasystem.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class Arena {
    private final String id;
    private String displayName;
    private BlockPos minPos;
    private BlockPos maxPos;
    private int maxTeams = 2;
    private int maxPlayersPerTeam = 1;
    private final Map<Integer, List<SpawnPoint>> teamSpawns = new HashMap<>();
    private SpawnPoint spectatorSpawn;
    private ArenaBlockSnapshot blockSnapshot;
    private ArenaBoundary boundary;

    public Arena(String id, String displayName, BlockPos minPos, BlockPos maxPos) {
        this.id = id;
        this.displayName = displayName;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.blockSnapshot = new ArenaBlockSnapshot(minPos, maxPos);
        this.boundary = new ArenaBoundary(minPos, maxPos);
    }

    public boolean isConfigured() {
        return getMaxSupportedTeams() >= 2 && spectatorSpawn != null;
    }

    public boolean supportsTeamCount(int count) {
        if (count < 2) return false;
        return count <= getMaxSupportedTeams();
    }

    public int getMaxSupportedTeams() {
        int count = 0;
        while (true) {
            List<SpawnPoint> spawns = teamSpawns.get(count + 1);
            if (spawns == null || spawns.isEmpty()) {
                break;
            }
            count++;
        }
        return count;
    }

    public boolean isInsideBoundary(double x, double y, double z) {
        return boundary == null || boundary.isInside(x, y, z);
    }

    public boolean isBelowVoid(double y) {
        return boundary != null && boundary.isBelowVoid(y);
    }

    public Vec3 getCenterVec() {
        return new Vec3(
                (minPos.getX() + maxPos.getX() + 1) / 2.0,
                minPos.getY() + 1.0,
                (minPos.getZ() + maxPos.getZ() + 1) / 2.0
        );
    }

    public void addTeamSpawn(int teamIndex, SpawnPoint spawn) {
        teamSpawns.computeIfAbsent(teamIndex, k -> new ArrayList<>()).add(spawn);
        if (teamIndex > maxTeams) {
            maxTeams = teamIndex;
        }
    }

    public void clearTeamSpawns(int teamIndex) {
        teamSpawns.remove(teamIndex);
    }

    public List<SpawnPoint> getTeamSpawns(int teamIndex) {
        return teamSpawns.getOrDefault(teamIndex, Collections.emptyList());
    }

    public void captureMapSnapshot(ServerLevel level) {
        if (blockSnapshot == null) {
            blockSnapshot = new ArenaBlockSnapshot(minPos, maxPos);
        }
        blockSnapshot.capture(level);
    }

    public void rollbackMap(ServerLevel level) {
        if (blockSnapshot != null) {
            blockSnapshot.rollback(level);
        }
    }

    /**
     * Creates a fully isolated, translated in-world copy of this arena template for a match.
     */
    public Arena createInstance(int instanceSlot, ServerLevel arenaLevel) {
        int offsetX = instanceSlot * 1000;
        int diffX = offsetX - minPos.getX();

        BlockPos instMin = new BlockPos(offsetX, minPos.getY(), minPos.getZ());
        BlockPos instMax = new BlockPos(maxPos.getX() + diffX, maxPos.getY(), maxPos.getZ());

        Arena inst = new Arena(id + "_inst_" + instanceSlot, displayName, instMin, instMax);
        inst.setMaxTeams(this.maxTeams);
        inst.setMaxPlayersPerTeam(this.maxPlayersPerTeam);

        // Copy blocks from snapshot to instanced position
        if (this.blockSnapshot != null) {
            this.blockSnapshot.pasteToOffset(arenaLevel, diffX, 0, 0);
            inst.captureMapSnapshot(arenaLevel);
        }

        // Translate team spawns
        for (Map.Entry<Integer, List<SpawnPoint>> entry : this.teamSpawns.entrySet()) {
            for (SpawnPoint sp : entry.getValue()) {
                inst.addTeamSpawn(entry.getKey(), new SpawnPoint(sp.getX() + diffX, sp.getY(), sp.getZ(), sp.getYaw(), sp.getPitch(), arenaLevel.dimension()));
            }
        }

        // Translate spectator spawn
        if (this.spectatorSpawn != null) {
            inst.setSpectatorSpawn(new SpawnPoint(this.spectatorSpawn.getX() + diffX, this.spectatorSpawn.getY(), this.spectatorSpawn.getZ(), this.spectatorSpawn.getYaw(), this.spectatorSpawn.getPitch(), arenaLevel.dimension()));
        }

        // Translate boundary
        if (this.boundary != null) {
            inst.setBoundary(this.boundary.createOffsetCopy(diffX, 0, 0, instMin, instMax));
        }

        return inst;
    }

    public void cleanupInstance(ServerLevel level) {
        if (blockSnapshot != null) {
            blockSnapshot.clearToAir(level);
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("displayName", displayName);
        obj.addProperty("maxTeams", getMaxSupportedTeams());
        obj.addProperty("maxPlayersPerTeam", maxPlayersPerTeam);

        JsonObject min = new JsonObject();
        min.addProperty("x", minPos.getX());
        min.addProperty("y", minPos.getY());
        min.addProperty("z", minPos.getZ());
        obj.add("minPos", min);

        JsonObject max = new JsonObject();
        max.addProperty("x", maxPos.getX());
        max.addProperty("y", maxPos.getY());
        max.addProperty("z", maxPos.getZ());
        obj.add("maxPos", max);

        JsonObject spawnsObj = new JsonObject();
        for (Map.Entry<Integer, List<SpawnPoint>> entry : teamSpawns.entrySet()) {
            JsonArray arr = new JsonArray();
            for (SpawnPoint sp : entry.getValue()) {
                arr.add(sp.toJson());
            }
            spawnsObj.add(entry.getKey().toString(), arr);
        }
        obj.add("teamSpawns", spawnsObj);

        if (spectatorSpawn != null) obj.add("spectatorSpawn", spectatorSpawn.toJson());
        if (boundary != null) obj.add("boundary", boundary.toJson());

        return obj;
    }

    public static Arena fromJson(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String name = obj.get("displayName").getAsString();

        JsonObject min = obj.getAsJsonObject("minPos");
        BlockPos minPos = new BlockPos(min.get("x").getAsInt(), min.get("y").getAsInt(), min.get("z").getAsInt());

        JsonObject max = obj.getAsJsonObject("maxPos");
        BlockPos maxPos = new BlockPos(max.get("x").getAsInt(), max.get("y").getAsInt(), max.get("z").getAsInt());

        Arena arena = new Arena(id, name, minPos, maxPos);
        if (obj.has("maxTeams")) arena.setMaxTeams(obj.get("maxTeams").getAsInt());
        if (obj.has("maxPlayersPerTeam")) arena.setMaxPlayersPerTeam(obj.get("maxPlayersPerTeam").getAsInt());

        if (obj.has("teamSpawns")) {
            JsonObject spawnsObj = obj.getAsJsonObject("teamSpawns");
            for (String key : spawnsObj.keySet()) {
                int team = Integer.parseInt(key);
                JsonArray arr = spawnsObj.getAsJsonArray(key);
                for (JsonElement el : arr) {
                    arena.addTeamSpawn(team, SpawnPoint.fromJson(el.getAsJsonObject()));
                }
            }
        }

        if (obj.has("spectatorSpawn")) {
            arena.setSpectatorSpawn(SpawnPoint.fromJson(obj.getAsJsonObject("spectatorSpawn")));
        }
        if (obj.has("boundary")) {
            arena.setBoundary(ArenaBoundary.fromJson(obj.getAsJsonObject("boundary"), minPos, maxPos));
        }

        return arena;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }
    public BlockPos getMinPos() { return minPos; }
    public BlockPos getMaxPos() { return maxPos; }
    public int getMaxTeams() { return maxTeams; }
    public void setMaxTeams(int maxTeams) { this.maxTeams = Math.max(2, maxTeams); }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) { this.maxPlayersPerTeam = Math.max(1, maxPlayersPerTeam); }
    public void setSpectatorSpawn(SpawnPoint sp) { this.spectatorSpawn = sp; }
    public SpawnPoint getSpectatorSpawn() { return spectatorSpawn; }
    public Map<Integer, List<SpawnPoint>> getAllTeamSpawns() { return teamSpawns; }
    public ArenaBoundary getBoundary() { return boundary; }
    public void setBoundary(ArenaBoundary boundary) { this.boundary = boundary; }
}