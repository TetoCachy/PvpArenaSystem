package com.tetocachy.pvparenasystem.player;

import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {
    private static final Map<UUID, PlayerSnapshot> activeSnapshots = new ConcurrentHashMap<>();

    private static Path getSnapshotDir(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("pvparenasystem").resolve("snapshots");
        File dir = path.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    public static void saveSnapshot(ServerPlayer player, String reason) {
        PlayerSnapshot snapshot = PlayerSnapshot.capture(player, reason);
        activeSnapshots.put(player.getUUID(), snapshot);

        // Persist to disk
        try {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                Path file = getSnapshotDir(server).resolve(player.getUUID() + ".dat");
                NbtIo.writeCompressed(snapshot.toNbt(), file);
            }
        } catch (Exception e) {
            PvpArenaSystem.LOGGER.error("Failed to save player snapshot to disk for {}", player.getScoreboardName(), e);
        }
    }

    public static boolean hasSnapshot(UUID uuid) {
        return activeSnapshots.containsKey(uuid);
    }

    public static void restorePlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PlayerSnapshot snapshot = activeSnapshots.remove(uuid);
        MinecraftServer server = player.level().getServer();

        if (snapshot == null && server != null) {
            try {
                Path file = getSnapshotDir(server).resolve(uuid + ".dat");
                if (file.toFile().exists()) {
                    CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                    snapshot = PlayerSnapshot.fromNbt(tag);
                }
            } catch (Exception e) {
                PvpArenaSystem.LOGGER.error("Failed to read snapshot from disk for {}", player.getScoreboardName(), e);
            }
        }

        if (snapshot != null) {
            snapshot.restore(player);
            if (server != null) {
                deleteSnapshotFile(server, uuid);
            }
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            Path file = getSnapshotDir(server).resolve(player.getUUID() + ".dat");
            if (file.toFile().exists()) {
                PvpArenaSystem.LOGGER.info("Restoring offline snapshot for player {}", player.getScoreboardName());
                restorePlayer(player);
            }
        }
    }

    private static void deleteSnapshotFile(MinecraftServer server, UUID uuid) {
        try {
            Path file = getSnapshotDir(server).resolve(uuid + ".dat");
            File f = file.toFile();
            if (f.exists()) {
                f.delete();
            }
        } catch (Exception ignored) {}
    }

    public static void emergencyRestoreAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasSnapshot(player.getUUID())) {
                restorePlayer(player);
            }
        }
    }
}