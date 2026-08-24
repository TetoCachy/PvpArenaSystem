package com.tetocachy.pvparenasystem.event;

import com.tetocachy.pvparenasystem.admin.SelectionManager;
import com.tetocachy.pvparenasystem.admin.SetupSession;
import com.tetocachy.pvparenasystem.config.ArenaModConfig;
import com.tetocachy.pvparenasystem.match.ArenaMatch;
import com.tetocachy.pvparenasystem.match.MatchManager;
import com.tetocachy.pvparenasystem.network.ModPackets;
import com.tetocachy.pvparenasystem.player.PlayerStateManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class PlayerEventListener {

    public static void register() {
        // 1. Wand Left-Click: Pos 1
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                MinecraftServer server = serverPlayer.level().getServer();
                if (server != null && server.getPlayerList().isOp(serverPlayer.nameAndId()) && SelectionManager.isWandEnabled(player.getUUID())) {
                    if (player.getItemInHand(hand).is(ArenaModConfig.WAND_ITEM)) {
                        SelectionManager.setPos1(serverPlayer, pos);
                        ModPackets.sendSyncToPlayer(serverPlayer);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        // 2. Wand Right-Click: Pos 2 or Spawn Selector in Setup Mode
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                MinecraftServer server = serverPlayer.level().getServer();
                if (server != null && server.getPlayerList().isOp(serverPlayer.nameAndId()) && SelectionManager.isWandEnabled(player.getUUID())) {
                    if (player.getItemInHand(hand).is(ArenaModConfig.WAND_ITEM)) {
                        if (SetupSession.isInSetup(player.getUUID())) {
                            // Open Spawn Selector directly in GUI
                            ModPackets.sendSyncToPlayer(serverPlayer);
                        } else {
                            SelectionManager.setPos2(serverPlayer, hitResult.getBlockPos());
                            ModPackets.sendSyncToPlayer(serverPlayer);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        // 3. Block Break protection during matches
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                if (MatchManager.isInMatch(serverPlayer.getUUID()) && !ArenaModConfig.ALLOW_BLOCK_BREAKING) {
                    return false;
                }
            }
            return true;
        });

        // 4. Intercept Fatal Damage in matches
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                ArenaMatch match = MatchManager.getPlayerMatch(player.getUUID());
                if (match != null) {
                    match.handlePlayerDeath(player);
                    return false;
                }
            }
            return true;
        });

        // 5. Connection Events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerStateManager.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            MatchManager.onPlayerDisconnect(handler.getPlayer());
            if (SetupSession.isInSetup(handler.getPlayer().getUUID())) {
                SetupSession.finishSetup(handler.getPlayer());
            }
        });

        // Under connection events:
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerStateManager.onPlayerJoin(handler.getPlayer());
            ModPackets.sendSyncToPlayer(handler.getPlayer()); // Syncs immediately upon loading in
        });
    }
}