package com.tetocachy.pvparenasystem.match;

import com.tetocachy.pvparenasystem.arena.Arena;
import com.tetocachy.pvparenasystem.arena.SpawnPoint;
import com.tetocachy.pvparenasystem.config.ArenaModConfig;
import com.tetocachy.pvparenasystem.dimension.ModDimensions;
import com.tetocachy.pvparenasystem.kit.Kit;
import com.tetocachy.pvparenasystem.player.PlayerStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaMatch {
    private final UUID matchId = UUID.randomUUID();
    private final MinecraftServer server;
    private final Arena arena;
    private final Kit kit;
    private final int roundsToWin;
    private final Map<Integer, TeamData> teams = new HashMap<>();
    private final Set<UUID> allPlayers = new HashSet<>();
    private final Map<UUID, Vec3> spawnFreezePositions = new ConcurrentHashMap<>();

    private MatchState state = MatchState.WAITING;
    private int countdownTimer = ArenaModConfig.COUNTDOWN_SECONDS * 20;
    private int celebrationTimer = ArenaModConfig.CELEBRATION_SECONDS * 20;
    private int intermissionTimer = 0;
    private int currentRound = 1;

    public ArenaMatch(MinecraftServer server, Arena arena, Kit kit, int roundsToWin, Map<Integer, List<UUID>> teamAssignments) {
        this.server = server;
        this.arena = arena;
        this.kit = kit;
        this.roundsToWin = Math.max(1, roundsToWin);

        ChatFormatting[] colors = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD, ChatFormatting.WHITE};
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
        spawnFreezePositions.clear();

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
                        spawnFreezePositions.put(uuid, new Vec3(sp.getX(), sp.getY(), sp.getZ()));
                    } else {
                        spawnFreezePositions.put(uuid, player.position());
                    }
                    spIndex++;
                }
            }
        }

        broadcast("§6[PvpArena] §eRound " + currentRound + " / " + (roundsToWin * 2 - 1) + " is starting!");
    }

    public void tick() {
        // 1. Boundary & Out-of-Bounds Check
        for (UUID uuid : allPlayers) {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p == null) continue;

            TeamData team = getPlayerTeam(uuid);
            boolean isAlive = team != null && team.isAlive(uuid);

            // Hard Void Rescue: Prevents falling below world bounds
            if (arena.isBelowVoid(p.getY())) {
                if (isAlive) {
                    handlePlayerDeath(p);
                } else {
                    teleportToSpectatorSpawn(p);
                }
                continue;
            }

            // Horizontal Boundary Containment
            if (isAlive && state == MatchState.IN_PROGRESS) {
                if (!arena.isInsideBoundary(p.getX(), p.getY(), p.getZ())) {
                    if (p.getY() < arena.getMinPos().getY()) {
                        handlePlayerDeath(p);
                    } else {
                        Vec3 center = arena.getCenterVec();
                        Vec3 dir = center.subtract(p.position()).normalize().scale(0.6);
                        p.setDeltaMovement(dir.x, 0.25, dir.z);
                        p.hurtMarked = true;
                        p.sendSystemMessage(Component.literal("§c§l[!] STAY INSIDE THE ARENA BORDER!"), true);
                        p.playSound(SoundEvents.PLAYER_HURT, 0.6F, 1.2F);
                    }
                }
            } else if (!isAlive) {
                if (p.getY() < -30 || p.distanceToSqr(arena.getCenterVec()) > 40000) {
                    teleportToSpectatorSpawn(p);
                }
            }
        }

        // 2. Pre-match countdown freeze
        if (state == MatchState.COUNTDOWN) {
            for (UUID uuid : allPlayers) {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                Vec3 freeze = spawnFreezePositions.get(uuid);
                if (p != null && freeze != null) {
                    if (p.distanceToSqr(freeze.x, freeze.y, freeze.z) > 0.05) {
                        p.teleportTo((ServerLevel) p.level(), freeze.x, freeze.y, freeze.z, Set.of(), p.getYRot(), p.getXRot(), true);
                        p.setDeltaMovement(0, 0, 0);
                    }
                }
            }

            if (countdownTimer % 20 == 0) {
                int secondsLeft = countdownTimer / 20;
                if (secondsLeft > 0) {
                    broadcastTitle("§e" + secondsLeft, "§7Get Ready!");
                    playSound(SoundEvents.NOTE_BLOCK_PLING, 1.0F, 1.0F);
                } else {
                    broadcastTitle("§a§lFIGHT!", "§eRound " + currentRound);
                    playSound(SoundEvents.NOTE_BLOCK_PLING, 1.0F, 2.0F);
                    state = MatchState.IN_PROGRESS;
                }
            }
            countdownTimer--;
        } else if (state == MatchState.ENDING) {
            if (intermissionTimer > 0) {
                intermissionTimer--;
                if (intermissionTimer % 20 == 0 && intermissionTimer > 0) {
                    broadcastTitle("§6Next Round", "§eStarting in " + (intermissionTimer / 20) + "s...");
                }
                if (intermissionTimer <= 0) {
                    currentRound++;
                    prepareRound();
                }
            } else {
                celebrationTimer--;
                if (celebrationTimer <= 0) {
                    cleanupAndEnd();
                }
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

        teleportToSpectatorSpawn(player);

        player.sendSystemMessage(Component.literal("§c§lYOU WERE ELIMINATED!"), true);
        broadcast(playerTeam.getColor() + player.getScoreboardName() + " §7was eliminated!");
        playSound(SoundEvents.WITHER_HURT, 0.8F, 1.5F);
        checkRoundOver();
    }

    public void teleportToSpectatorSpawn(ServerPlayer player) {
        if (arena.getSpectatorSpawn() != null) {
            arena.getSpectatorSpawn().teleport(player);
        } else {
            Vec3 center = arena.getCenterVec();
            ServerLevel level = ModDimensions.getArenaLevel(server);
            player.teleportTo(level, center.x, arena.getMaxPos().getY() + 4.0, center.z, Set.of(), 0.0F, 0.0F, true);
        }
        player.setDeltaMovement(0, 0, 0);
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
                broadcast("§6§l[Round Over] " + winner.getColor() + winner.getName() + " §awon Round " + currentRound + "! §7(Score: " + winner.getScore() + "/" + roundsToWin + ")");

                if (winner.getScore() >= roundsToWin) {
                    state = MatchState.ENDING;
                    celebrationTimer = ArenaModConfig.CELEBRATION_SECONDS * 20;
                    broadcastTitle("§6§lVICTORY!", winner.getColor() + winner.getName() + " won the match!");
                    playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                    return;
                }
            } else {
                broadcast("§eRound ended in a Draw!");
            }

            state = MatchState.ENDING;
            intermissionTimer = 60;
            broadcastTitle("§6Round Complete", "§7Next round in 3 seconds...");
            playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        }
    }

    public void forfeitPlayer(ServerPlayer player) {
        handlePlayerDeath(player);
        PlayerStateManager.restorePlayer(player);
        allPlayers.remove(player.getUUID());
    }

    public void cleanupAndEnd() {
        state = MatchState.RESETTING;

        for (UUID uuid : allPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                PlayerStateManager.restorePlayer(player);
                player.sendSystemMessage(Component.literal("§a[PvpArena] Match concluded! Returned to your original state."), false);
            }
        }

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

    public boolean hasPlayer(UUID uuid) { return allPlayers.contains(uuid); }

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

    public void playSound(Holder<SoundEvent> sound, float volume, float pitch) {
        playSound(sound.value(), volume, pitch);
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
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