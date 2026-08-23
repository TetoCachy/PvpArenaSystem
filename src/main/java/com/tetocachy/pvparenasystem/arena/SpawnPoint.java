package com.tetocachy.pvparenasystem.arena;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;

public class SpawnPoint {
    private final double x, y, z;
    private final float yaw, pitch;
    private final ResourceKey<Level> dimension;

    public SpawnPoint(double x, double y, double z, float yaw, float pitch, ResourceKey<Level> dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }

    public static SpawnPoint fromPlayer(ServerPlayer player) {
        return new SpawnPoint(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.level().dimension());
    }

    public void teleport(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) level = server.overworld();
            player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("z", z);
        obj.addProperty("yaw", yaw);
        obj.addProperty("pitch", pitch);
        obj.addProperty("dim", dimension.identifier().toString());
        return obj;
    }

    public static SpawnPoint fromJson(JsonObject obj) {
        return new SpawnPoint(
                obj.get("x").getAsDouble(),
                obj.get("y").getAsDouble(),
                obj.get("z").getAsDouble(),
                obj.get("yaw").getAsFloat(),
                obj.get("pitch").getAsFloat(),
                ResourceKey.create(Registries.DIMENSION, Identifier.parse(obj.get("dim").getAsString()))
        );
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
}