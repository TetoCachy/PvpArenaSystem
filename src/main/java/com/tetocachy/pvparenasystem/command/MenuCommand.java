package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.tetocachy.pvparenasystem.match.MatchManager;
import com.tetocachy.pvparenasystem.network.ModPackets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MenuCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pvp")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (MatchManager.isInMatch(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("§c[!] You cannot open the PvP menu while fighting in a match!"), false);
                        return 0;
                    }
                    ModPackets.sendSyncToPlayer(player);
                    return 1;
                }));

        dispatcher.register(Commands.literal("menu")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (MatchManager.isInMatch(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("§c[!] You cannot open the PvP menu while fighting in a match!"), false);
                        return 0;
                    }
                    ModPackets.sendSyncToPlayer(player);
                    return 1;
                }));
    }
}