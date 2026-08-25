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
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

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

        // 2. Wand Right-Click on Block: Shift = Clear Selection, Pos 2 or Spawn Selector in Setup Mode
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                MinecraftServer server = serverPlayer.level().getServer();
                if (server != null && server.getPlayerList().isOp(serverPlayer.nameAndId()) && SelectionManager.isWandEnabled(player.getUUID())) {
                    if (player.getItemInHand(hand).is(ArenaModConfig.WAND_ITEM)) {
                        if (player.isShiftKeyDown() || player.isCrouching()) {
                            SelectionManager.clearSelection(serverPlayer);
                            ModPackets.sendSyncToPlayer(serverPlayer);
                            return InteractionResult.SUCCESS;
                        }
                        if (SetupSession.isInSetup(player.getUUID())) {
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

        // 2b. Wand Right-Click in Air: Shift = Clear Selection
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayer serverPlayer && !world.isClientSide()) {
                if (player.isShiftKeyDown() || player.isCrouching()) {
                    if (player.getItemInHand(hand).is(ArenaModConfig.WAND_ITEM)) {
                        MinecraftServer server = serverPlayer.level().getServer();
                        if (server != null && server.getPlayerList().isOp(serverPlayer.nameAndId()) && SelectionManager.isWandEnabled(player.getUUID())) {
                            SelectionManager.clearSelection(serverPlayer);
                            ModPackets.sendSyncToPlayer(serverPlayer);
                            return InteractionResult.SUCCESS;
                        }
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

        // 4. Intercept Friendly Fire in Matches
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer victim) {
                Entity attacker = damageSource.getEntity();
                if (attacker instanceof ServerPlayer attackerPlayer) {
                    ArenaMatch match = MatchManager.getPlayerMatch(victim.getUUID());
                    if (match != null && match.hasPlayer(attackerPlayer.getUUID())) {
                        if (!match.isFriendlyFire() && match.areOnSameTeam(victim.getUUID(), attackerPlayer.getUUID())) {
                            return false;
                        }
                    }
                }
            }
            return true;
        });

        // 5. Intercept Fatal Damage in matches
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

        // 6. Connection Events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerStateManager.onPlayerJoin(handler.getPlayer());
            ModPackets.sendSyncToPlayer(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            MatchManager.onPlayerDisconnect(handler.getPlayer());
            if (SetupSession.isInSetup(handler.getPlayer().getUUID())) {
                SetupSession.finishSetup(handler.getPlayer());
            }
        });
    }
}