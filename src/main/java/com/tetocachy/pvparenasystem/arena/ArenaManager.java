package com.tetocachy.pvparenasystem.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.tetocachy.pvparenasystem.PvpArenaSystem;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private static int arenaOffsetIndex = 0;

    private static Path getArenasDir(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("pvparenasystem").resolve("arenas");
        File dir = path.toFile();
        if (!dir.exists()) dir.mkdirs();
        return path;
    }

    public static void loadArenas(MinecraftServer server) {
        arenas.clear();
        Path dir = getArenasDir(server);
        File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".json"));
        ServerLevel arenaLevel = ModDimensions.getArenaLevel(server);

        if (files != null) {
            for (File file : files) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                    Arena arena = Arena.fromJson(obj);
                    arena.captureMapSnapshot(arenaLevel);
                    arenas.put(arena.getId().toLowerCase(), arena);
                } catch (Exception e) {
                    PvpArenaSystem.LOGGER.error("Failed to load arena " + file.getName(), e);
                }
            }
        }
        PvpArenaSystem.LOGGER.info("Loaded {} PvP Arenas.", arenas.size());
    }

    public static void saveArena(MinecraftServer server, Arena arena) {
        arenas.put(arena.getId().toLowerCase(), arena);
        try {
            Path file = getArenasDir(server).resolve(arena.getId().toLowerCase() + ".json");
            try (FileWriter writer = new FileWriter(file.toFile())) {
                GSON.toJson(arena.toJson(), writer);
            }
        } catch (Exception e) {
            PvpArenaSystem.LOGGER.error("Failed to save arena " + arena.getId(), e);
        }
    }

    public static Arena createArenaFromSelection(MinecraftServer server, String id, String displayName,
                                                 ServerLevel sourceLevel, BlockPos p1, BlockPos p2) {
        BlockPos min = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        BlockPos max = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;

        // Allocate isolated plot in Arena Dimension
        int targetOffsetX = arenaOffsetIndex * 500;
        int targetOffsetY = 64;
        int targetOffsetZ = 0;
        arenaOffsetIndex++;

        ServerLevel arenaLevel = ModDimensions.getArenaLevel(server);
        BlockPos targetMin = new BlockPos(targetOffsetX, targetOffsetY, targetOffsetZ);
        BlockPos targetMax = new BlockPos(targetOffsetX + sizeX - 1, targetOffsetY + sizeY - 1, targetOffsetZ + sizeZ - 1);

        // Copy blocks to arena dimension
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos src = min.offset(x, y, z);
                    BlockPos dest = targetMin.offset(x, y, z);
                    BlockState state = sourceLevel.getBlockState(src);
                    arenaLevel.setBlock(dest, state, 2);
                }
            }
        }

        Arena arena = new Arena(id, displayName, targetMin, targetMax);
        arena.captureMapSnapshot(arenaLevel);
        saveArena(server, arena);
        return arena;
    }

    public static Arena getArena(String id) {
        return arenas.get(id.toLowerCase());
    }

    public static Arena getAvailableArena(String preferred) {
        if (preferred != null && arenas.containsKey(preferred.toLowerCase())) {
            Arena a = arenas.get(preferred.toLowerCase());
            if (!a.isInUse() && a.isConfigured()) return a;
        }
        for (Arena a : arenas.values()) {
            if (!a.isInUse() && a.isConfigured()) return a;
        }
        return null;
    }

    public static Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public static boolean deleteArena(MinecraftServer server, String id) {
        Arena a = arenas.remove(id.toLowerCase());
        if (a != null) {
            Path file = getArenasDir(server).resolve(id.toLowerCase() + ".json");
            File f = file.toFile();
            if (f.exists()) f.delete();
            return true;
        }
        return false;
    }
}