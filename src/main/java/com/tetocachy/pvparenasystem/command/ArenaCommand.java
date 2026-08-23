package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tetocachy.pvparenasystem.admin.SelectionManager;
import com.tetocachy.pvparenasystem.admin.SetupSession;
import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.arena.SpawnPoint;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ArenaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arena")
                .then(Commands.literal("wand")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            SelectionManager.toggleWand(player);
                            return 1;
                        }))
                .then(Commands.literal("create")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    BlockPos p1 = SelectionManager.getPos1(player.getUUID());
                                    BlockPos p2 = SelectionManager.getPos2(player.getUUID());

                                    if (p1 == null || p2 == null) {
                                        player.sendSystemMessage(Component.literal("§cPlease select Pos 1 and Pos 2 first using your wand!"), false);
                                        return 0;
                                    }

                                    Arena arena = ArenaManager.createArenaFromSelection(ctx.getSource().getServer(), name, name, (ServerLevel) player.level(), p1, p2);
                                    SetupSession.startSetup(player, arena);
                                    return 1;
                                })))
                .then(Commands.literal("edit")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Arena arena = ArenaManager.getArena(name);
                                    if (arena == null) {
                                        player.sendSystemMessage(Component.literal("§cArena not found!"), false);
                                        return 0;
                                    }
                                    SetupSession.startSetup(player, arena);
                                    return 1;
                                })))
                .then(Commands.literal("setspawn")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int team = IntegerArgumentType.getInteger(ctx, "team");
                                    Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                    if (arena == null) {
                                        player.sendSystemMessage(Component.literal("§cYou are not in arena setup mode!"), false);
                                        return 0;
                                    }
                                    arena.addTeamSpawn(team, SpawnPoint.fromPlayer(player));
                                    player.sendSystemMessage(Component.literal("§aSpawn for Team " + team + " added at your position!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("setspectator")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                            if (arena == null) {
                                player.sendSystemMessage(Component.literal("§cYou are not in arena setup mode!"), false);
                                return 0;
                            }
                            arena.setSpectatorSpawn(SpawnPoint.fromPlayer(player));
                            player.sendSystemMessage(Component.literal("§aSpectator spawn set!"), false);
                            return 1;
                        }))
                .then(Commands.literal("save")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                            if (arena == null) {
                                player.sendSystemMessage(Component.literal("§cYou are not in setup mode!"), false);
                                return 0;
                            }
                            arena.captureMapSnapshot(ModDimensions.getArenaLevel(ctx.getSource().getServer()));
                            ArenaManager.saveArena(ctx.getSource().getServer(), arena);
                            player.sendSystemMessage(Component.literal("§aArena saved successfully!"), false);
                            return 1;
                        }))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (SetupSession.isInSetup(player.getUUID())) {
                                SetupSession.finishSetup(player);
                                return 1;
                            }
                            return 0;
                        }))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSystemMessage(Component.literal("§6§lAvailable Arenas:"));
                            for (Arena a : ArenaManager.getAllArenas()) {
                                String status = a.isInUse() ? "§c[IN USE]" : (a.isConfigured() ? "§a[READY]" : "§e[SETUP]");
                                source.sendSystemMessage(Component.literal("§f- §e" + a.getDisplayName() + " " + status));
                            }
                            return 1;
                        }))
        );
    }
}