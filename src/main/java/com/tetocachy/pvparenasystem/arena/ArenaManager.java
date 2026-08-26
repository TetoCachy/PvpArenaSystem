package com.tetocachy.pvparenasystem.arena;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.tetocachy.pvparenasystem.PvpArenaSystem;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
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
    private static final Set<Integer> occupiedSlots = ConcurrentHashMap.newKeySet();
    private static int templateOffsetIndex = 0;

    private static Path getArenasDir(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("pvparenasystem").resolve("arenas");
        File dir = path.toFile();
        if (!dir.exists()) dir.mkdirs();
        return path;
    }

    public static void loadArenas(MinecraftServer server) {
        arenas.clear();
        occupiedSlots.clear();
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

        // Base Template origin in void
        int targetOffsetX = templateOffsetIndex * 500;
        int targetOffsetY = 64;
        int targetOffsetZ = 0;
        templateOffsetIndex++;

        ServerLevel arenaLevel = ModDimensions.getArenaLevel(server);
        BlockPos targetMin = new BlockPos(targetOffsetX, targetOffsetY - 1, targetOffsetZ);
        BlockPos targetMax = new BlockPos(targetOffsetX + sizeX - 1, targetOffsetY + sizeY - 1, targetOffsetZ + sizeZ - 1);

        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                boolean hasNonAirBlock = false;
                for (int y = 0; y < sizeY; y++) {
                    BlockPos src = min.offset(x, y, z);
                    BlockPos dest = new BlockPos(targetOffsetX + x, targetOffsetY + y, targetOffsetZ + z);
                    BlockState state = sourceLevel.getBlockState(src);
                    arenaLevel.setBlock(dest, state, 2);
                    if (!state.isAir()) {
                        hasNonAirBlock = true;
                    }
                }

                BlockPos bedrockPos = new BlockPos(targetOffsetX + x, targetOffsetY - 1, targetOffsetZ + z);
                if (hasNonAirBlock) {
                    arenaLevel.setBlock(bedrockPos, Blocks.BEDROCK.defaultBlockState(), 2);
                } else {
                    arenaLevel.setBlock(bedrockPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        Arena arena = new Arena(id, displayName, targetMin, targetMax);
        arena.captureMapSnapshot(arenaLevel);
        saveArena(server, arena);
        return arena;
    }

    /**
     * Allocates a dynamic instance copy of a blueprint arena.
     */
    public static synchronized Arena createMatchInstance(MinecraftServer server, Arena template) {
        int slot = 100; // Instances start at X = 100,000+ to never collide with templates
        while (occupiedSlots.contains(slot)) {
            slot++;
        }
        occupiedSlots.add(slot);

        ServerLevel level = ModDimensions.getArenaLevel(server);
        return template.createInstance(slot, level);
    }

    public static synchronized void releaseInstance(ServerLevel level, Arena instance) {
        if (instance != null) {
            instance.cleanupInstance(level);
            // Extract slot index from id: "<template>_inst_<slot>"
            try {
                String[] parts = instance.getId().split("_inst_");
                if (parts.length > 1) {
                    int slot = Integer.parseInt(parts[1]);
                    occupiedSlots.remove(slot);
                }
            } catch (Exception ignored) {}
        }
    }

    public static Arena getArena(String id) {
        return arenas.get(id.toLowerCase());
    }

    public static Arena getAvailableArena(String preferred, int requiredTeams) {
        if (preferred != null && arenas.containsKey(preferred.toLowerCase())) {
            Arena a = arenas.get(preferred.toLowerCase());
            if (a.isConfigured() && a.supportsTeamCount(requiredTeams)) return a;
        }
        for (Arena a : arenas.values()) {
            if (a.isConfigured() && a.supportsTeamCount(requiredTeams)) return a;
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