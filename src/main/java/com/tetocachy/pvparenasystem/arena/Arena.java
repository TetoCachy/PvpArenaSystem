package com.tetocachy.pvparenasystem.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

public class Arena {
    private final String id;
    private String displayName;
    private BlockPos minPos;
    private BlockPos maxPos;
    private final Map<Integer, List<SpawnPoint>> teamSpawns = new HashMap<>();
    private SpawnPoint spectatorSpawn;
    private SpawnPoint lobbySpawn;
    private boolean inUse = false;
    private ArenaBlockSnapshot blockSnapshot;

    public Arena(String id, String displayName, BlockPos minPos, BlockPos maxPos) {
        this.id = id;
        this.displayName = displayName;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.blockSnapshot = new ArenaBlockSnapshot(minPos, maxPos);
    }

    public boolean isConfigured() {
        return teamSpawns.containsKey(1) && teamSpawns.containsKey(2) && spectatorSpawn != null;
    }

    public void addTeamSpawn(int teamIndex, SpawnPoint spawn) {
        teamSpawns.computeIfAbsent(teamIndex, k -> new ArrayList<>()).add(spawn);
    }

    public List<SpawnPoint> getTeamSpawns(int teamIndex) {
        return teamSpawns.getOrDefault(teamIndex, Collections.emptyList());
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minPos.getX() && pos.getX() <= maxPos.getX()
                && pos.getY() >= minPos.getY() && pos.getY() <= maxPos.getY()
                && pos.getZ() >= minPos.getZ() && pos.getZ() <= maxPos.getZ();
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

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("displayName", displayName);

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
        if (lobbySpawn != null) obj.add("lobbySpawn", lobbySpawn.toJson());

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
        if (obj.has("lobbySpawn")) {
            arena.setLobbySpawn(SpawnPoint.fromJson(obj.getAsJsonObject("lobbySpawn")));
        }

        return arena;
    }

    // Getters & Setters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public BlockPos getMinPos() { return minPos; }
    public BlockPos getMaxPos() { return maxPos; }
    public void setSpectatorSpawn(SpawnPoint sp) { this.spectatorSpawn = sp; }
    public SpawnPoint getSpectatorSpawn() { return spectatorSpawn; }
    public void setLobbySpawn(SpawnPoint sp) { this.lobbySpawn = sp; }
    public SpawnPoint getLobbySpawn() { return lobbySpawn; }
    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
    public Map<Integer, List<SpawnPoint>> getAllTeamSpawns() { return teamSpawns; }
}