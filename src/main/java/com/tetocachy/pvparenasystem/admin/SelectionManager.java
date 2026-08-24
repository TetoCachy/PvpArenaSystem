package com.tetocachy.pvparenasystem.admin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionManager {
    private static final Map<UUID, BlockPos> pos1Map = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> pos2Map = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> wandEnabled = new ConcurrentHashMap<>();

    public static void setPos1(ServerPlayer player, BlockPos pos) {
        pos1Map.put(player.getUUID(), pos);
        player.sendSystemMessage(Component.literal("§a[PvpArena] Pos 1 set to: §f(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"), false);
        checkSelection(player);
    }

    public static void setPos2(ServerPlayer player, BlockPos pos) {
        pos2Map.put(player.getUUID(), pos);
        player.sendSystemMessage(Component.literal("§a[PvpArena] Pos 2 set to: §f(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"), false);
        checkSelection(player);
    }

    public static void clearSelection(ServerPlayer player) {
        clearSelectionSilently(player.getUUID());
        player.sendSystemMessage(Component.literal("§e[PvpArena] Selection cleared!"), false);
    }

    public static void clearSelectionSilently(UUID uuid) {
        pos1Map.remove(uuid);
        pos2Map.remove(uuid);
    }

    private static void checkSelection(ServerPlayer player) {
        BlockPos p1 = pos1Map.get(player.getUUID());
        BlockPos p2 = pos2Map.get(player.getUUID());
        if (p1 != null && p2 != null) {
            int dx = Math.abs(p1.getX() - p2.getX()) + 1;
            int dy = Math.abs(p1.getY() - p2.getY()) + 1;
            int dz = Math.abs(p1.getZ() - p2.getZ()) + 1;
            int total = dx * dy * dz;
            player.sendSystemMessage(Component.literal("§eSelection bounds: §b" + dx + "x" + dy + "x" + dz + " §7(" + total + " blocks)"), false);
        }
    }

    public static BlockPos getPos1(UUID uuid) { return pos1Map.get(uuid); }
    public static BlockPos getPos2(UUID uuid) { return pos2Map.get(uuid); }

    public static boolean isWandEnabled(UUID uuid) {
        return wandEnabled.getOrDefault(uuid, true);
    }

    public static void toggleWand(ServerPlayer player) {
        boolean next = !isWandEnabled(player.getUUID());
        wandEnabled.put(player.getUUID(), next);
        player.sendSystemMessage(Component.literal("§6[PvpArena] Selection Wand mode: " + (next ? "§aENABLED" : "§cDISABLED")), false);
    }
}