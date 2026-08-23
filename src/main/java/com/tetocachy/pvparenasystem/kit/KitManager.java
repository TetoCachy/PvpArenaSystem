package com.tetocachy.pvparenasystem.kit;

import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KitManager {
    private static final Map<String, Kit> kits = new ConcurrentHashMap<>();

    private static Path getKitsDir(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("pvparenasystem").resolve("kits");
        File dir = path.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    public static void loadKits(MinecraftServer server) {
        kits.clear();
        Path dir = getKitsDir(server);
        File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".kit"));
        if (files != null) {
            for (File file : files) {
                try {
                    CompoundTag tag = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
                    Kit kit = Kit.fromNbt(tag);
                    kits.put(kit.getId().toLowerCase(), kit);
                } catch (Exception e) {
                    PvpArenaSystem.LOGGER.error("Failed to load kit file: " + file.getName(), e);
                }
            }
        }
        PvpArenaSystem.LOGGER.info("Loaded {} custom kits.", kits.size());
    }

    public static void saveKit(MinecraftServer server, Kit kit) {
        kits.put(kit.getId().toLowerCase(), kit);
        try {
            Path file = getKitsDir(server).resolve(kit.getId().toLowerCase() + ".kit");
            NbtIo.writeCompressed(kit.toNbt(), file);
        } catch (Exception e) {
            PvpArenaSystem.LOGGER.error("Failed to save kit: " + kit.getId(), e);
        }
    }

    public static Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public static Collection<Kit> getAllKits() {
        return Collections.unmodifiableCollection(kits.values());
    }

    public static boolean deleteKit(MinecraftServer server, String id) {
        Kit removed = kits.remove(id.toLowerCase());
        if (removed != null) {
            Path file = getKitsDir(server).resolve(id.toLowerCase() + ".kit");
            File f = file.toFile();
            if (f.exists()) f.delete();
            return true;
        }
        return false;
    }
}