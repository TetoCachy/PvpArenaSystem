package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.kit.KitManager;
import com.tetocachy.pvparenasystem.match.MatchManager;
import com.tetocachy.pvparenasystem.party.Party;
import com.tetocachy.pvparenasystem.party.PartyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class PartyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .then(Commands.literal("create")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PartyManager.createParty(player.getUUID());
                            player.sendSystemMessage(Component.literal("§aParty created! Use §6/party invite <player> §ato invite teammates."), false);
                            return 1;
                        }))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    Party party = PartyManager.getParty(player.getUUID());
                                    if (party == null || !party.getLeader().equals(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal("§cYou are not the party leader!"), false);
                                        return 0;
                                    }
                                    party.addMember(target.getUUID());
                                    target.sendSystemMessage(Component.literal("§aYou joined §e" + player.getScoreboardName() + "'s §aparty!"), false);
                                    player.sendSystemMessage(Component.literal("§e" + target.getScoreboardName() + " §ahas joined the party!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Party party = PartyManager.getParty(player.getUUID());
                            if (party != null) {
                                party.removeMember(player.getUUID());
                                player.sendSystemMessage(Component.literal("§7You left the party."), false);
                                return 1;
                            }
                            return 0;
                        }))
                .then(Commands.literal("duel")
                        .then(Commands.argument("targetLeader", EntityArgument.player())
                                .then(Commands.argument("kit", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer leader1 = ctx.getSource().getPlayerOrException();
                                            ServerPlayer leader2 = EntityArgument.getPlayer(ctx, "targetLeader");
                                            String kitName = StringArgumentType.getString(ctx, "kit");

                                            Party p1 = PartyManager.getParty(leader1.getUUID());
                                            Party p2 = PartyManager.getParty(leader2.getUUID());

                                            if (p1 == null || p2 == null) {
                                                leader1.sendSystemMessage(Component.literal("§cBoth teams must be in a party!"), false);
                                                return 0;
                                            }

                                            Arena arena = ArenaManager.getAvailableArena(null);
                                            if (arena == null) {
                                                leader1.sendSystemMessage(Component.literal("§cNo available PvP arenas!"), false);
                                                return 0;
                                            }

                                            Kit kit = KitManager.getKit(kitName);

                                            Map<Integer, List<UUID>> teams = new HashMap<>();
                                            teams.put(1, new ArrayList<>(p1.getMembers()));
                                            teams.put(2, new ArrayList<>(p2.getMembers()));

                                            MatchManager.createMatch(ctx.getSource().getServer(), arena, kit, 1, teams);
                                            return 1;
                                        }))))
        );
    }
}