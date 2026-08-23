package com.tetocachy.pvparenasystem.dimension;

import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final ResourceKey<Level> ARENA_DIMENSION_KEY =
            ResourceKey.create(Registries.DIMENSION, PvpArenaSystem.id("arena_dimension"));

    public static ServerLevel getArenaLevel(MinecraftServer server) {
        ServerLevel level = server.getLevel(ARENA_DIMENSION_KEY);
        if (level == null) {
            // Fallback to overworld if dimension failed to load
            PvpArenaSystem.LOGGER.warn("Arena dimension not found, falling back to Overworld!");
            return server.overworld();
        }
        return level;
    }
}