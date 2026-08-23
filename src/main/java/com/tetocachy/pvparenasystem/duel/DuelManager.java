package com.tetocachy.pvparenasystem.duel;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.kit.KitManager;
import com.tetocachy.pvparenasystem.match.MatchManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {
    private static final Map<UUID, List<DuelChallenge>> pendingChallenges = new ConcurrentHashMap<>();

    public static void sendChallenge(ServerPlayer sender, ServerPlayer target, String kitName, String arenaName, int rounds) {
        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("§cYou cannot challenge yourself!"), false);
            return;
        }

        if (MatchManager.isInMatch(sender.getUUID()) || MatchManager.isInMatch(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("§cOne of the players is already in a match!"), false);
            return;
        }

        DuelChallenge challenge = new DuelChallenge(sender.getUUID(), target.getUUID(), kitName, arenaName, rounds);
        pendingChallenges.computeIfAbsent(target.getUUID(), k -> new ArrayList<>()).add(challenge);

        Component acceptBtn = Component.literal(" [ACCEPT] ")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/duel accept " + sender.getScoreboardName()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept duel")))
                );

        Component declineBtn = Component.literal(" [DECLINE] ")
                .withStyle(style -> style
                        .withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/duel decline " + sender.getScoreboardName()))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to decline duel")))
                );

        Component inviteMsg = Component.literal("§6§l[PvP Duel] §e" + sender.getScoreboardName() + " §7has challenged you to a duel!")
                .append(Component.literal("\n§7Kit: §b" + (kitName != null ? kitName : "Default") + " §7| Arena: §b" + (arenaName != null ? arenaName : "Random") + " §7| Rounds: §b" + rounds + "\n"))
                .append(acceptBtn)
                .append(declineBtn);

        target.sendSystemMessage(inviteMsg, false);
        sender.sendSystemMessage(Component.literal("§aDuel invitation sent to §e" + target.getScoreboardName() + "§a!"), false);
    }

    public static void acceptChallenge(ServerPlayer target, String senderName) {
        List<DuelChallenge> list = pendingChallenges.get(target.getUUID());
        if (list == null || list.isEmpty()) {
            target.sendSystemMessage(Component.literal("§cYou have no pending duel challenges!"), false);
            return;
        }

        MinecraftServer server = target.level().getServer();
        if (server == null) return;

        DuelChallenge found = null;
        for (DuelChallenge c : list) {
            ServerPlayer s = server.getPlayerList().getPlayer(c.getSender());
            if (s != null && s.getScoreboardName().equalsIgnoreCase(senderName) && !c.isExpired()) {
                found = c;
                break;
            }
        }

        if (found == null) {
            target.sendSystemMessage(Component.literal("§cChallenge not found or expired!"), false);
            return;
        }

        list.remove(found);
        ServerPlayer sender = server.getPlayerList().getPlayer(found.getSender());
        if (sender == null) {
            target.sendSystemMessage(Component.literal("§cChallenger is offline!"), false);
            return;
        }

        Arena arena = ArenaManager.getAvailableArena(found.getArenaName());
        if (arena == null) {
            target.sendSystemMessage(Component.literal("§cNo available PvP arenas found!"), false);
            sender.sendSystemMessage(Component.literal("§cNo available PvP arenas found!"), false);
            return;
        }

        Kit kit = found.getKitName() != null ? KitManager.getKit(found.getKitName()) : null;

        Map<Integer, List<UUID>> teams = new HashMap<>();
        teams.put(1, List.of(sender.getUUID()));
        teams.put(2, List.of(target.getUUID()));

        MatchManager.createMatch(server, arena, kit, found.getRounds(), teams);
    }

    public static void declineChallenge(ServerPlayer target, String senderName) {
        List<DuelChallenge> list = pendingChallenges.get(target.getUUID());
        MinecraftServer server = target.level().getServer();
        if (list != null && server != null) {
            list.removeIf(c -> {
                ServerPlayer s = server.getPlayerList().getPlayer(c.getSender());
                return s != null && s.getScoreboardName().equalsIgnoreCase(senderName);
            });
        }
        target.sendSystemMessage(Component.literal("§7Duel declined."), false);
    }
}