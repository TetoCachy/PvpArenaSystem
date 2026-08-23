package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.kit.KitManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class KitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kit")
                .then(Commands.literal("save")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Kit kit = Kit.fromPlayer(name, name, player);
                                    KitManager.saveKit(ctx.getSource().getServer(), kit);
                                    player.sendSystemMessage(Component.literal("§a[PvpArena] Kit '§e" + name + "§a' saved from your current inventory!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("load")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Kit kit = KitManager.getKit(name);
                                    if (kit == null) {
                                        player.sendSystemMessage(Component.literal("§cKit not found!"), false);
                                        return 0;
                                    }
                                    kit.apply(player);
                                    player.sendSystemMessage(Component.literal("§aLoaded kit '§e" + name + "§a'!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("delete")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (KitManager.deleteKit(ctx.getSource().getServer(), name)) {
                                        ctx.getSource().sendSystemMessage(Component.literal("§aDeleted kit " + name));
                                        return 1;
                                    }
                                    ctx.getSource().sendSystemMessage(Component.literal("§cKit not found."));
                                    return 0;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendSystemMessage(Component.literal("§6§lRegistered Kits:"));
                            for (Kit k : KitManager.getAllKits()) {
                                ctx.getSource().sendSystemMessage(Component.literal("§f- §b" + k.getDisplayName()));
                            }
                            return 1;
                        }))
        );
    }
}