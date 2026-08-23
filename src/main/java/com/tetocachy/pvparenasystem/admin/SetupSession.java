package com.tetocachy.pvparenasystem.admin;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import com.tetocachy.pvparenasystem.player.PlayerStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SetupSession {
    private static final Map<UUID, Arena> activeSessions = new ConcurrentHashMap<>();

    public static void startSetup(ServerPlayer player, Arena arena) {
        UUID uuid = player.getUUID();
        if (activeSessions.containsKey(uuid)) {
            finishSetup(player);
        }

        PlayerStateManager.saveSnapshot(player, "ARENA_SETUP");
        activeSessions.put(uuid, arena);

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ServerLevel arenaLevel = ModDimensions.getArenaLevel(server);
            BlockPos min = arena.getMinPos();
            player.setGameMode(GameType.CREATIVE);
            player.teleportTo(arenaLevel, min.getX() + 0.5, min.getY() + 1.0, min.getZ() + 0.5, Set.of(), 0.0F, 0.0F, true);
        }

        player.sendSystemMessage(Component.literal("§a========================================"), false);
        player.sendSystemMessage(Component.literal("§6§lPvP Arena Setup Mode: §e" + arena.getDisplayName()), false);
        player.sendSystemMessage(Component.literal("§f- Stand where Team 1 spawns and run: §6/arena setspawn 1"), false);
        player.sendSystemMessage(Component.literal("§f- Stand where Team 2 spawns and run: §6/arena setspawn 2"), false);
        player.sendSystemMessage(Component.literal("§f- (Optional: /arena setspawn 3, 4 ... for multi-teams)"), false);
        player.sendSystemMessage(Component.literal("§f- Stand where spectators watch and run: §6/arena setspectator"), false);
        player.sendSystemMessage(Component.literal("§f- To commit changes: §a/arena save"), false);
        player.sendSystemMessage(Component.literal("§f- To leave setup: §c/arena leave"), false);
        player.sendSystemMessage(Component.literal("§a========================================"), false);
    }

    public static Arena getCurrentEditingArena(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public static boolean isInSetup(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public static void finishSetup(ServerPlayer player) {
        Arena arena = activeSessions.remove(player.getUUID());
        MinecraftServer server = player.level().getServer();
        if (arena != null && server != null) {
            ArenaManager.saveArena(server, arena);
        }
        PlayerStateManager.restorePlayer(player);
        player.sendSystemMessage(Component.literal("§a[PvpArena] Left arena setup mode. Your original inventory has been restored!"), false);
    }
}