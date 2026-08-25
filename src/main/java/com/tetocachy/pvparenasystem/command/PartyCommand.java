package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tetocachy.pvparenasystem.party.Party;
import com.tetocachy.pvparenasystem.party.PartyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PartyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("party")
                .then(Commands.literal("create")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PartyManager.createParty(player);
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
                                    PartyManager.joinParty(target, party, ctx.getSource().getServer());
                                    return 1;
                                })))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PartyManager.leaveParty(player, ctx.getSource().getServer());
                            return 1;
                        }))
        );
    }
}