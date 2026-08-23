package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tetocachy.pvparenasystem.duel.DuelManager;
import com.tetocachy.pvparenasystem.match.ArenaMatch;
import com.tetocachy.pvparenasystem.match.MatchManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DuelCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("duel")
                .then(Commands.literal("accept")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String sender = StringArgumentType.getString(ctx, "player");
                                    DuelManager.acceptChallenge(player, sender);
                                    return 1;
                                })))
                .then(Commands.literal("decline")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String sender = StringArgumentType.getString(ctx, "player");
                                    DuelManager.declineChallenge(player, sender);
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ArenaMatch match = MatchManager.getPlayerMatch(player.getUUID());
                            if (match != null) {
                                match.forfeitPlayer(player);
                                player.sendSystemMessage(Component.literal("§cYou forfeited and left the match."), false);
                                return 1;
                            }
                            player.sendSystemMessage(Component.literal("§cYou are not in a match."), false);
                            return 0;
                        }))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            DuelManager.sendChallenge(sender, target, null, null, 1);
                            return 1;
                        })
                        .then(Commands.argument("kit", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                    String kit = StringArgumentType.getString(ctx, "kit");
                                    DuelManager.sendChallenge(sender, target, kit, null, 1);
                                    return 1;
                                })
                                .then(Commands.argument("arena", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            String kit = StringArgumentType.getString(ctx, "kit");
                                            String arena = StringArgumentType.getString(ctx, "arena");
                                            DuelManager.sendChallenge(sender, target, kit, arena, 1);
                                            return 1;
                                        })
                                        .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> {
                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    String kit = StringArgumentType.getString(ctx, "kit");
                                                    String arena = StringArgumentType.getString(ctx, "arena");
                                                    int rounds = IntegerArgumentType.getInteger(ctx, "rounds");
                                                    DuelManager.sendChallenge(sender, target, kit, arena, rounds);
                                                    return 1;
                                                }))))
                )
        );
    }
}