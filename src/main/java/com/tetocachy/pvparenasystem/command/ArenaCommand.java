package com.tetocachy.pvparenasystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tetocachy.pvparenasystem.admin.SelectionManager;
import com.tetocachy.pvparenasystem.admin.SetupSession;
import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaBoundary;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.arena.SpawnPoint;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import com.tetocachy.pvparenasystem.match.MatchManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

public class ArenaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arena")
                .executes(ctx -> sendHelpMenu(ctx.getSource()))
                .then(Commands.literal("help")
                        .executes(ctx -> sendHelpMenu(ctx.getSource())))
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
                                        player.sendSystemMessage(Component.literal("§c[!] Please select Pos 1 and Pos 2 first using your wand (or Wooden Pickaxe)!"), false);
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
                                        player.sendSystemMessage(Component.literal("§c[!] Arena '" + name + "' not found!"), false);
                                        return 0;
                                    }
                                    SetupSession.startSetup(player, arena);
                                    return 1;
                                })))
                .then(Commands.literal("delete")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (ArenaManager.deleteArena(ctx.getSource().getServer(), name)) {
                                        ctx.getSource().sendSystemMessage(Component.literal("§a[PvpArena] Successfully deleted arena '§e" + name + "§a'."));
                                        return 1;
                                    } else {
                                        ctx.getSource().sendSystemMessage(Component.literal("§c[!] Arena '" + name + "' not found."));
                                        return 0;
                                    }
                                })))
                .then(Commands.literal("setspawn")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int team = IntegerArgumentType.getInteger(ctx, "team");
                                    Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                    if (arena == null) {
                                        player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode (/arena edit <name>) to set spawns!"), false);
                                        return 0;
                                    }
                                    arena.addTeamSpawn(team, SpawnPoint.fromPlayer(player));
                                    player.sendSystemMessage(Component.literal("§a[Setup] Spawn for Team " + team + " added at your position!"), false);
                                    return 1;
                                })))
                .then(Commands.literal("clearspawns")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("team", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int team = IntegerArgumentType.getInteger(ctx, "team");
                                    Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                    if (arena == null) {
                                        player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode (/arena edit <name>) to modify spawns!"), false);
                                        return 0;
                                    }
                                    arena.clearTeamSpawns(team);
                                    player.sendSystemMessage(Component.literal("§e[Setup] Cleared all spawns for Team " + team + "."), false);
                                    return 1;
                                })))
                .then(Commands.literal("setspectator")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                            if (arena == null) {
                                player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode (/arena edit <name>) to set spectator spawn!"), false);
                                return 0;
                            }
                            arena.setSpectatorSpawn(SpawnPoint.fromPlayer(player));
                            player.sendSystemMessage(Component.literal("§a[Setup] Spectator spawn set at your position!"), false);
                            return 1;
                        }))
                .then(Commands.literal("border")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("mode")
                                .then(Commands.argument("shape", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                            if (arena == null) {
                                                player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode to modify borders!"), false);
                                                return 0;
                                            }
                                            String s = StringArgumentType.getString(ctx, "shape").toUpperCase();
                                            try {
                                                ArenaBoundary.Shape shape = ArenaBoundary.Shape.valueOf(s);
                                                arena.getBoundary().setShape(shape);
                                                player.sendSystemMessage(Component.literal("§a[Setup] Border shape set to: §e" + shape.name()), false);
                                                return 1;
                                            } catch (Exception e) {
                                                player.sendSystemMessage(Component.literal("§c[!] Invalid shape! Choose BOX, CYLINDER, or POLYGON"), false);
                                                return 0;
                                            }
                                        })))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(5.0, 1000.0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                            if (arena == null) {
                                                player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode to modify border radius!"), false);
                                                return 0;
                                            }
                                            double r = DoubleArgumentType.getDouble(ctx, "value");
                                            arena.getBoundary().setRadius(r);
                                            player.sendSystemMessage(Component.literal("§a[Setup] Cylinder border radius set to: §e" + r), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("addpoint")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                    if (arena == null) {
                                        player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode to add polygon border points!"), false);
                                        return 0;
                                    }
                                    arena.getBoundary().addPolygonPoint(player.getX(), player.getZ());
                                    player.sendSystemMessage(Component.literal("§a[Setup] Added polygon vertex at (" + String.format("%.1f", player.getX()) + ", " + String.format("%.1f", player.getZ()) + ") [Total: " + arena.getBoundary().getPolygonPoints().size() + "]"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("clearpoints")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                                    if (arena == null) {
                                        player.sendSystemMessage(Component.literal("§c[!] You must be in setup mode to clear border points!"), false);
                                        return 0;
                                    }
                                    arena.getBoundary().clearPolygonPoints();
                                    player.sendSystemMessage(Component.literal("§e[Setup] Cleared all polygon border points."), false);
                                    return 1;
                                })))
                .then(Commands.literal("save")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            Arena arena = SetupSession.getCurrentEditingArena(player.getUUID());
                            if (arena == null) {
                                player.sendSystemMessage(Component.literal("§c[!] You are not in setup mode!"), false);
                                return 0;
                            }
                            arena.captureMapSnapshot(ModDimensions.getArenaLevel(ctx.getSource().getServer()));
                            ArenaManager.saveArena(ctx.getSource().getServer(), arena);
                            player.sendSystemMessage(Component.literal("§a[PvpArena] Arena '§e" + arena.getDisplayName() + "§a' saved successfully!"), false);
                            return 1;
                        }))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (SetupSession.isInSetup(player.getUUID())) {
                                SetupSession.finishSetup(player);
                                return 1;
                            }
                            player.sendSystemMessage(Component.literal("§c[!] You are not currently in setup mode."), false);
                            return 0;
                        }))
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Arena a = ArenaManager.getArena(name);
                                    if (a == null) {
                                        ctx.getSource().sendSystemMessage(Component.literal("§c[!] Arena '" + name + "' not found!"));
                                        return 0;
                                    }
                                    sendArenaInfo(ctx.getSource(), a);
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSystemMessage(Component.literal("§6§l=== Registered Arenas (" + ArenaManager.getAllArenas().size() + ") ==="));
                            if (ArenaManager.getAllArenas().isEmpty()) {
                                source.sendSystemMessage(Component.literal("§7No arenas created yet. Use /arena create <name> to make one."));
                            } else {
                                for (Arena a : ArenaManager.getAllArenas()) {
                                    int active = MatchManager.getActiveFightsForArena(a.getId());
                                    String status = a.isConfigured() ? "§a[READY]" : "§e[SETUP NEEDED]";
                                    String activeText = active > 0 ? " §6(" + active + " Active Fights)" : "";
                                    String teams = a.isConfigured() ? "§7(2-" + a.getMaxSupportedTeams() + " Teams)" : "§c(Incomplete)";
                                    source.sendSystemMessage(Component.literal("§f- §e" + a.getDisplayName() + " " + status + activeText + " " + teams));
                                }
                            }
                            return 1;
                        }))
        );
    }

    private static int sendHelpMenu(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal("§6§l================== [ PvP Arena Commands ] =================="));
        source.sendSystemMessage(Component.literal("§eGeneral & Player Commands:"));
        source.sendSystemMessage(Component.literal("  §f/arena list §7- View all registered arenas and status"));
        source.sendSystemMessage(Component.literal("  §f/arena info <name> §7- View full details & configuration of an arena"));
        source.sendSystemMessage(Component.literal("  §f/arena leave §7- Exit arena setup mode and restore your items"));
        source.sendSystemMessage(Component.literal("  §f/arena help §7- Display this help menu"));
        source.sendSystemMessage(Component.literal("  §f/pvp §7or §f/menu §7(or Key §b[K]§7) - Open the main GUI"));

        if (Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)) {
            source.sendSystemMessage(Component.literal("§6Admin Setup & Management:"));
            source.sendSystemMessage(Component.literal("  §f/arena wand §7- Toggle region selection wand mode"));
            source.sendSystemMessage(Component.literal("  §f/arena create <name> §7- Clone wand selection to void & enter setup"));
            source.sendSystemMessage(Component.literal("  §f/arena edit <name> §7- Re-enter setup mode for an existing arena"));
            source.sendSystemMessage(Component.literal("  §f/arena delete <name> §7- Permanently delete an arena"));
            source.sendSystemMessage(Component.literal("  §f/arena save §7- Save map snapshot & configuration to disk"));

            source.sendSystemMessage(Component.literal("§6Spawn Configuration (In Setup Mode):"));
            source.sendSystemMessage(Component.literal("  §f/arena setspawn <team> §7- Add team spawn (e.g. 1, 2, 3...) at your feet"));
            source.sendSystemMessage(Component.literal("  §f/arena clearspawns <team> §7- Clear all spawns for a team"));
            source.sendSystemMessage(Component.literal("  §f/arena setspectator §7- Set spectator spawn position"));

            source.sendSystemMessage(Component.literal("§6Border Customization (In Setup Mode):"));
            source.sendSystemMessage(Component.literal("  §f/arena border mode <BOX|CYLINDER|POLYGON> §7- Change boundary type"));
            source.sendSystemMessage(Component.literal("  §f/arena border radius <val> §7- Set radius for cylindrical border"));
            source.sendSystemMessage(Component.literal("  §f/arena border addpoint §7- Add current (X, Z) as polygon vertex"));
            source.sendSystemMessage(Component.literal("  §f/arena border clearpoints §7- Reset polygon boundary points"));
        }
        source.sendSystemMessage(Component.literal("§6§l=========================================================="));
        return 1;
    }

    private static void sendArenaInfo(CommandSourceStack source, Arena a) {
        int sx = Math.abs(a.getMaxPos().getX() - a.getMinPos().getX()) + 1;
        int sy = Math.abs(a.getMaxPos().getY() - a.getMinPos().getY()) + 1;
        int sz = Math.abs(a.getMaxPos().getZ() - a.getMinPos().getZ()) + 1;
        int active = MatchManager.getActiveFightsForArena(a.getId());
        String status = a.isConfigured() ? "§aReady §7(" + active + " Active Fights)" : "§eSetup Needed";

        source.sendSystemMessage(Component.literal("§6§l=== Arena Details: §e" + a.getDisplayName() + " §6§l==="));
        source.sendSystemMessage(Component.literal("§7ID: §f" + a.getId() + " §7| Status: " + status));
        source.sendSystemMessage(Component.literal("§7Dimensions: §f" + sx + "x" + sy + "x" + sz + " blocks"));
        source.sendSystemMessage(Component.literal("§7Supported Teams: §b2 to " + a.getMaxSupportedTeams() + " Teams"));
        source.sendSystemMessage(Component.literal("§7Max Players Per Team: §b" + a.getMaxPlayersPerTeam()));
        source.sendSystemMessage(Component.literal("§7Spectator Spawn: " + (a.getSpectatorSpawn() != null ? "§a✔ Configured" : "§c✖ Missing")));
        source.sendSystemMessage(Component.literal("§7Border Shape: §f" + (a.getBoundary() != null ? a.getBoundary().getShape().name() : "BOX")));

        source.sendSystemMessage(Component.literal("§eTeam Spawns:"));
        for (Map.Entry<Integer, List<SpawnPoint>> entry : a.getAllTeamSpawns().entrySet()) {
            source.sendSystemMessage(Component.literal("  §f• Team " + entry.getKey() + ": §a" + entry.getValue().size() + " spawn points"));
        }
    }
}