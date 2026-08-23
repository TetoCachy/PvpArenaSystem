package com.tetocachy.pvparenasystem.match;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.SpawnPoint;
import com.tetocachy.pvparenasystem.config.ArenaModConfig;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.player.PlayerStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;

import java.util.*;

public class ArenaMatch {
    private final UUID matchId = UUID.randomUUID();
    private final MinecraftServer server;
    private final Arena arena;
    private final Kit kit;
    private final int roundsToWin;
    private final Map<Integer, TeamData> teams = new HashMap<>();
    private final Set<UUID> allPlayers = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();

    private MatchState state = MatchState.WAITING;
    private int countdownTimer = ArenaModConfig.COUNTDOWN_SECONDS * 20;
    private int celebrationTimer = ArenaModConfig.CELEBRATION_SECONDS * 20;
    private int currentRound = 1;

    public ArenaMatch(MinecraftServer server, Arena arena, Kit kit, int roundsToWin, Map<Integer, List<UUID>> teamAssignments) {
        this.server = server;
        this.arena = arena;
        this.kit = kit;
        this.roundsToWin = roundsToWin;

        ChatFormatting[] colors = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE};
        int cIndex = 0;
        for (Map.Entry<Integer, List<UUID>> entry : teamAssignments.entrySet()) {
            int tIndex = entry.getKey();
            ChatFormatting color = colors[cIndex % colors.length];
            teams.put(tIndex, new TeamData(tIndex, "Team " + tIndex, color, entry.getValue()));
            allPlayers.addAll(entry.getValue());
            cIndex++;
        }
    }

    public void startMatch() {
        arena.setInUse(true);

        for (UUID uuid : allPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                PlayerStateManager.saveSnapshot(player, "MATCH_" + matchId);
                player.setGameMode(GameType.ADVENTURE);
            }
        }

        prepareRound();
    }

    private void prepareRound() {
        state = MatchState.COUNTDOWN;
        countdownTimer = ArenaModConfig.COUNTDOWN_SECONDS * 20;

        for (TeamData team : teams.values()) {
            team.resetRound();
            List<SpawnPoint> spawns = arena.getTeamSpawns(team.getTeamIndex());
            int spIndex = 0;

            for (UUID uuid : team.getMembers()) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    player.removeAllEffects();
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                    player.setGameMode(GameType.ADVENTURE);

                    if (kit != null) {
                        kit.apply(player);
                    }

                    if (!spawns.isEmpty()) {
                        SpawnPoint sp = spawns.get(spIndex % spawns.size());
                        sp.teleport(player);
                    }
                    spIndex++;
                }
            }
        }

        broadcast("§6[PvpArena] §eRound " + currentRound + " is starting!");
    }

    public void tick() {
        if (state == MatchState.COUNTDOWN) {
            if (countdownTimer % 20 == 0) {
                int secondsLeft = countdownTimer / 20;
                if (secondsLeft > 0) {
                    broadcastTitle("§e" + secondsLeft, "§7Get Ready!");
                    playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
                } else {
                    broadcastTitle("§a§lFIGHT!", "§eRound " + currentRound);
                    playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 2.0F);
                    state = MatchState.IN_PROGRESS;
                }
            }
            countdownTimer--;
        } else if (state == MatchState.ENDING) {
            celebrationTimer--;
            if (celebrationTimer <= 0) {
                cleanupAndEnd();
            }
        }
    }

    public void handlePlayerDeath(ServerPlayer player) {
        UUID uuid = player.getUUID();
        TeamData playerTeam = getPlayerTeam(uuid);
        if (playerTeam == null || !playerTeam.isAlive(uuid)) return;

        playerTeam.markEliminated(uuid);
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.setGameMode(GameType.SPECTATOR);

        if (arena.getSpectatorSpawn() != null) {
            arena.getSpectatorSpawn().teleport(player);
        }

        broadcast(playerTeam.getColor() + player.getScoreboardName() + " §7was eliminated!");
        checkRoundOver();
    }

    private void checkRoundOver() {
        List<TeamData> survivingTeams = new ArrayList<>();
        for (TeamData team : teams.values()) {
            if (!team.isTeamEliminated()) {
                survivingTeams.add(team);
            }
        }

        if (survivingTeams.size() <= 1) {
            if (survivingTeams.size() == 1) {
                TeamData winner = survivingTeams.get(0);
                winner.incrementScore();
                broadcast(winner.getColor() + winner.getName() + " §awon Round " + currentRound + "!");

                if (winner.getScore() >= roundsToWin) {
                    // Match Won!
                    state = MatchState.ENDING;
                    celebrationTimer = ArenaModConfig.CELEBRATION_SECONDS * 20;
                    broadcastTitle("§6§lVICTORY!", winner.getColor() + winner.getName() + " won the match!");
                    playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                    return;
                }
            } else {
                broadcast("§eRound ended in a Draw!");
            }

            // Next round
            currentRound++;
            prepareRound();
        }
    }

    public void forfeitPlayer(ServerPlayer player) {
        handlePlayerDeath(player);
        PlayerStateManager.restorePlayer(player);
        allPlayers.remove(player.getUUID());
    }

    public void cleanupAndEnd() {
        state = MatchState.RESETTING;

        // Restore all players safely
        for (UUID uuid : allPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                PlayerStateManager.restorePlayer(player);
                player.sendSystemMessage(Component.literal("§a[PvpArena] Match concluded! Returned to your original state."), false);
            }
        }

        // Rollback arena
        ServerLevel arenaLevel = ModDimensions.getArenaLevel(server);
        arena.rollbackMap(arenaLevel);
        arena.setInUse(false);

        MatchManager.removeMatch(matchId);
    }

    public TeamData getPlayerTeam(UUID uuid) {
        for (TeamData team : teams.values()) {
            if (team.getMembers().contains(uuid)) return team;
        }
        return null;
    }

    public boolean hasPlayer(UUID uuid) {
        return allPlayers.contains(uuid);
    }

    public void broadcast(String message) {
        Component comp = Component.literal(message);
        for (UUID uuid : allPlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) p.sendSystemMessage(comp, false);
        }
    }

    public void broadcastTitle(String title, String subtitle) {
        for (UUID uuid : allPlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                p.sendSystemMessage(Component.literal(title + " §r- " + subtitle), true);
            }
        }
    }

    public void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        for (UUID uuid : allPlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) {
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
            }
        }
    }

    public MatchState getState() { return state; }
    public UUID getMatchId() { return matchId; }
    public Arena getArena() { return arena; }
}