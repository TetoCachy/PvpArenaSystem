package com.tetocachy.pvparenasystem.client.gui.tabs;

import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
import com.tetocachy.pvparenasystem.client.gui.ArenaMainMenuScreen;
import com.tetocachy.pvparenasystem.client.gui.ArenaScreenTab;
import com.tetocachy.pvparenasystem.network.C2SActionPayload;
import com.tetocachy.pvparenasystem.network.S2CSyncArenaDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LobbiesTab implements ArenaScreenTab {
    private final ArenaMainMenuScreen parentScreen;
    private int contentX, contentY, contentW, contentH;
    private int selectedArenaIndex = 0;
    private int selectedKitIndex = 0;

    private int teamCountIndex = 0;
    private int teamSizeIndex = 0;
    private int goalPointsIndex = 1;
    private boolean friendlyFire = false;

    private int teamScrollIndex = 0;

    public LobbiesTab(ArenaMainMenuScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public Component getTitle() { return Component.literal("Lobbies & Matchmaking"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.DIAMOND_SWORD); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = width;
        this.contentH = height;

        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload data = ClientArenaCache.currentData;

        List<Integer> teamCounts = data.allowedTeamCounts().isEmpty() ? List.of(2, 3, 4) : data.allowedTeamCounts();
        List<Integer> teamSizes = data.allowedTeamSizes().isEmpty() ? List.of(1, 2, 4) : data.allowedTeamSizes();
        List<Integer> goalPoints = data.allowedGoalPoints().isEmpty() ? List.of(1, 2, 3, 5) : data.allowedGoalPoints();

        teamCountIndex = Math.min(teamCountIndex, teamCounts.size() - 1);
        teamSizeIndex = Math.min(teamSizeIndex, teamSizes.size() - 1);
        goalPointsIndex = Math.min(goalPointsIndex, goalPoints.size() - 1);

        int currentTeamCount = teamCounts.get(teamCountIndex);
        int currentTeamSize = teamSizes.get(teamSizeIndex);
        int currentGoalPoints = goalPoints.get(goalPointsIndex);

        if (data.currentLobby() != null) {
            S2CSyncArenaDataPayload.LobbyInfo l = data.currentLobby();
            int totalTeams = Math.max(1, l.teams().size());
            int gap = 6;
            int viewW = width - 8;

            int visibleTeamsCount = Math.min(3, totalTeams);
            int cardW = (viewW - ((visibleTeamsCount - 1) * gap)) / visibleTeamsCount;

            int maxScrollIndex = Math.max(0, totalTeams - visibleTeamsCount);
            teamScrollIndex = Math.max(0, Math.min(maxScrollIndex, teamScrollIndex));

            if (maxScrollIndex > 0) {
                addWidget.accept(Button.builder(Component.literal("◀"), b -> {
                    if (teamScrollIndex > 0) {
                        teamScrollIndex--;
                        parentScreen.rebuildTabContent();
                    }
                }).bounds(x + width - 44, y + 2, 18, 18).build());

                addWidget.accept(Button.builder(Component.literal("▶"), b -> {
                    if (teamScrollIndex < maxScrollIndex) {
                        teamScrollIndex++;
                        parentScreen.rebuildTabContent();
                    }
                }).bounds(x + width - 24, y + 2, 18, 18).build());
            }

            for (int col = 0; col < visibleTeamsCount; col++) {
                int teamListIdx = teamScrollIndex + col;
                if (teamListIdx >= totalTeams) break;

                final int teamIdx = teamListIdx + 1;
                int cardX = x + 4 + (col * (cardW + gap));

                Button switchBtn = Button.builder(Component.literal("Join Team " + teamIdx), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_SWITCH_TEAM", "", "", teamIdx, 0));
                }).bounds(cardX, y + height - 48, cardW, 18).build();
                addWidget.accept(switchBtn);
            }

            addWidget.accept(Button.builder(Component.literal("👁 Become Spectator"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("LOBBY_BECOME_SPECTATOR", "", "", 0, 0));
            }).bounds(x + 4, y + height - 70, width - 8, 18).build());

            boolean isHost = l.hostName().equalsIgnoreCase(Minecraft.getInstance().getUser().getName());
            if (isHost) {
                addWidget.accept(Button.builder(Component.literal("§a§l▶ START MATCH"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_START", "", "", 0, 0));
                }).bounds(x + 4, y + height - 24, (width / 2) - 6, 20).build());

                addWidget.accept(Button.builder(Component.literal("§c✕ Disband Lobby"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_LEAVE", "", "", 0, 0));
                }).bounds(x + (width / 2) + 2, y + height - 24, (width / 2) - 6, 20).build());
            } else {
                addWidget.accept(Button.builder(Component.literal("§c✕ Leave Lobby"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_LEAVE", "", "", 0, 0));
                }).bounds(x + 4, y + height - 24, width - 8, 20).build());
            }

        } else {
            boolean isPartyMemberNotLeader = data.party().inParty() && !data.party().isLeader();

            List<S2CSyncArenaDataPayload.ArenaInfo> compatibleArenas = new ArrayList<>();
            for (S2CSyncArenaDataPayload.ArenaInfo a : data.arenas()) {
                if (a.maxTeams() >= currentTeamCount || a.teamSpawnCount() >= currentTeamCount) {
                    compatibleArenas.add(a);
                }
            }

            if (selectedArenaIndex >= compatibleArenas.size()) {
                selectedArenaIndex = 0;
            }

            String curArena = compatibleArenas.isEmpty() ? ("§cNo Map for " + currentTeamCount + "T") : ("§e" + compatibleArenas.get(selectedArenaIndex).displayName());
            List<S2CSyncArenaDataPayload.KitInfo> kits = data.kits();
            String curKit = kits.isEmpty() ? "Default Kit" : kits.get(Math.min(selectedKitIndex, Math.max(0, kits.size() - 1))).displayName();

            int colW = (width - 12) / 2;

            Button arenaBtn = Button.builder(Component.literal("Map: " + curArena), b -> {
                if (!compatibleArenas.isEmpty()) {
                    selectedArenaIndex = (selectedArenaIndex + 1) % compatibleArenas.size();
                    b.setMessage(Component.literal("Map: §e" + compatibleArenas.get(selectedArenaIndex).displayName()));
                }
            }).bounds(x + 4, y + 2, colW, 18).build();
            arenaBtn.active = !compatibleArenas.isEmpty();
            addWidget.accept(arenaBtn);

            addWidget.accept(Button.builder(Component.literal("Kit: §b" + curKit), b -> {
                if (!kits.isEmpty()) {
                    selectedKitIndex = (selectedKitIndex + 1) % kits.size();
                    b.setMessage(Component.literal("Kit: §b" + kits.get(selectedKitIndex).displayName()));
                }
            }).bounds(x + colW + 8, y + 2, colW, 18).build());

            int fourthW = (width - 20) / 4;

            // Teams Selector (Modular Preset Cycle)
            addWidget.accept(Button.builder(Component.literal("Teams: §a" + currentTeamCount), b -> {
                teamCountIndex = (teamCountIndex + 1) % teamCounts.size();
                selectedArenaIndex = 0;
                parentScreen.rebuildTabContent();
            }).bounds(x + 4, y + 22, fourthW, 18).build());

            // Team Size Selector (Modular Preset Cycle)
            addWidget.accept(Button.builder(Component.literal("Size: §a" + currentTeamSize + "v" + currentTeamSize), b -> {
                teamSizeIndex = (teamSizeIndex + 1) % teamSizes.size();
                int nextSize = teamSizes.get(teamSizeIndex);
                b.setMessage(Component.literal("Size: §a" + nextSize + "v" + nextSize));
            }).bounds(x + fourthW + 8, y + 22, fourthW, 18).build());

            // Goal Points Selector (Modular Preset Cycle with Pt / Pts grammar)
            String pointLabel = currentGoalPoints == 1 ? "1 Pt" : currentGoalPoints + " Pts";
            addWidget.accept(Button.builder(Component.literal("Goal: §e" + pointLabel), b -> {
                goalPointsIndex = (goalPointsIndex + 1) % goalPoints.size();
                int nextPts = goalPoints.get(goalPointsIndex);
                b.setMessage(Component.literal("Goal: §e" + (nextPts == 1 ? "1 Pt" : nextPts + " Pts")));
            }).bounds(x + (fourthW * 2) + 12, y + 22, fourthW, 18).build());

            addWidget.accept(Button.builder(Component.literal("FF: " + (friendlyFire ? "§aON" : "§cOFF")), b -> {
                friendlyFire = !friendlyFire;
                b.setMessage(Component.literal("FF: " + (friendlyFire ? "§aON" : "§cOFF")));
            }).bounds(x + (fourthW * 3) + 16, y + 22, fourthW, 18).build());

            if (isPartyMemberNotLeader) {
                Button lockedCreate = Button.builder(Component.literal("§c🔒 Only Party Leader Can Host"), b -> {}).bounds(x + 4, y + 43, width - 8, 20).build();
                lockedCreate.active = false;
                addWidget.accept(lockedCreate);
            } else if (compatibleArenas.isEmpty()) {
                Button noArenaCreate = Button.builder(Component.literal("§c✕ No Arena Supports " + currentTeamCount + " Teams"), b -> {}).bounds(x + 4, y + 43, width - 8, 20).build();
                noArenaCreate.active = false;
                addWidget.accept(noArenaCreate);
            } else {
                addWidget.accept(Button.builder(Component.literal("§a§l+ CREATE & OPEN LOBBY"), b -> {
                    String aId = compatibleArenas.get(selectedArenaIndex).id();
                    String kId = kits.isEmpty() ? "" : kits.get(Math.min(selectedKitIndex, kits.size() - 1)).id();
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_CREATE", aId, kId, currentTeamCount, currentTeamSize, currentGoalPoints, friendlyFire ? 1 : 0));
                }).bounds(x + 4, y + 43, width - 8, 20).build());
            }

            int listY = y + 78;
            for (S2CSyncArenaDataPayload.LobbyInfo l : data.lobbies()) {
                int totalInLobby = 0;
                for (S2CSyncArenaDataPayload.TeamSlotInfo t : l.teams()) {
                    totalInLobby += t.memberNames().size();
                }
                int maxCapacity = l.teamCount() * l.playersPerTeam();
                boolean isFull = totalInLobby >= maxCapacity;

                int joinBtnW = 60;
                int specBtnW = 55;
                int labelW = width - 8 - joinBtnW - specBtnW - 4;

                String pUnit = l.pointsToWin() == 1 ? "Pt" : "Pts";
                if (isPartyMemberNotLeader) {
                    Button lockedJoin = Button.builder(Component.literal("§7" + l.hostName() + "'s Lobby §c[Locked: In Party]"), b -> {}).bounds(x + 4, listY, width - 8, 18).build();
                    lockedJoin.active = false;
                    addWidget.accept(lockedJoin);
                } else {
                    String label = "§e" + l.hostName() + " §7[" + l.arenaName() + "] §6(" + l.pointsToWin() + " " + pUnit + ") §b(" + totalInLobby + "/" + maxCapacity + ")";
                    Button infoBtn = Button.builder(Component.literal(label), b -> {}).bounds(x + 4, listY, labelW, 18).build();
                    infoBtn.active = false;
                    addWidget.accept(infoBtn);

                    if (isFull) {
                        Button fullBtn = Button.builder(Component.literal("§cFULL"), b -> {}).bounds(x + 4 + labelW + 2, listY, joinBtnW, 18).build();
                        fullBtn.active = false;
                        addWidget.accept(fullBtn);
                    } else {
                        addWidget.accept(Button.builder(Component.literal("§a[JOIN]"), b -> {
                            ClientPlayNetworking.send(new C2SActionPayload("LOBBY_JOIN", l.lobbyId(), "", 0, 0));
                        }).bounds(x + 4 + labelW + 2, listY, joinBtnW, 18).build());
                    }

                    addWidget.accept(Button.builder(Component.literal("§b👁 Watch"), b -> {
                        ClientPlayNetworking.send(new C2SActionPayload("LOBBY_JOIN_SPECTATOR", l.lobbyId(), "", 0, 0));
                    }).bounds(x + 4 + labelW + joinBtnW + 4, listY, specBtnW, 18).build());
                }
                listY += 20;
            }

            if (!data.activeMatches().isEmpty()) {
                listY += 4;
                for (S2CSyncArenaDataPayload.OngoingMatchInfo match : data.activeMatches()) {
                    String pUnit = match.pointsToWin() == 1 ? "Pt" : "Pts";
                    String mLabel = "§6⚔ " + match.arenaName() + " §7(Round " + match.currentRound() + " - First to " + match.pointsToWin() + " " + pUnit + ") §b[" + match.playerCount() + " Fighters]";
                    int mBtnW = 75;
                    int mTextW = width - 8 - mBtnW - 2;

                    Button mInfo = Button.builder(Component.literal(mLabel), b -> {}).bounds(x + 4, listY, mTextW, 18).build();
                    mInfo.active = false;
                    addWidget.accept(mInfo);

                    addWidget.accept(Button.builder(Component.literal("§e👁 Spectate"), b -> {
                        ClientPlayNetworking.send(new C2SActionPayload("MATCH_SPECTATE", match.matchId(), "", 0, 0));
                    }).bounds(x + 4 + mTextW + 2, listY, mBtnW, 18).build());

                    listY += 20;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!ClientArenaCache.hasData() || ClientArenaCache.currentData.currentLobby() == null) return false;
        S2CSyncArenaDataPayload.LobbyInfo l = ClientArenaCache.currentData.currentLobby();
        int totalTeams = Math.max(1, l.teams().size());
        if (totalTeams <= 3) return false;

        int maxScrollIndex = totalTeams - 3;
        if (maxScrollIndex > 0 && mouseY >= contentY + 24 && mouseY <= contentY + contentH - 24) {
            if (verticalAmount > 0 && teamScrollIndex > 0) {
                teamScrollIndex--;
                parentScreen.rebuildTabContent();
                return true;
            } else if (verticalAmount < 0 && teamScrollIndex < maxScrollIndex) {
                teamScrollIndex++;
                parentScreen.rebuildTabContent();
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload data = ClientArenaCache.currentData;

        if (data.currentLobby() != null) {
            S2CSyncArenaDataPayload.LobbyInfo l = data.currentLobby();
            int totalTeams = Math.max(1, l.teams().size());
            int gap = 6;
            int viewW = contentW - 8;

            int visibleTeamsCount = Math.min(3, totalTeams);
            int cardW = (viewW - ((visibleTeamsCount - 1) * gap)) / visibleTeamsCount;

            graphics.fill(contentX + 4, contentY + 2, contentX + contentW - 4, contentY + 20, 0xF0161820);
            graphics.outline(contentX + 4, contentY + 2, contentW - 8, 18, 0xFF4A4E5C);

            String ffText = l.friendlyFire() ? "§aON" : "§cOFF";
            String pUnit = l.pointsToWin() == 1 ? "Pt" : "Pts";
            String header = "§6Host: §f" + l.hostName() + "  §7|  §eMap: §f" + l.arenaName() + "  §7|  §bKit: §f" + l.kitName() + "  §7|  §aFirst to " + l.pointsToWin() + " " + pUnit + "  §7|  §cFF: " + ffText;
            if (totalTeams > 3) {
                header += "  §e(" + (teamScrollIndex + 1) + "-" + (teamScrollIndex + visibleTeamsCount) + " of " + totalTeams + ")";
            }
            graphics.text(Minecraft.getInstance().font, Component.literal(header), contentX + 8, contentY + 7, 0xFFFFFFFF, false);

            int cardTop = contentY + 24;
            int cardH = contentH - 98;

            for (int col = 0; col < visibleTeamsCount; col++) {
                int teamListIdx = teamScrollIndex + col;
                if (teamListIdx >= totalTeams) break;

                S2CSyncArenaDataPayload.TeamSlotInfo t = l.teams().get(teamListIdx);
                int cardX = contentX + 4 + (col * (cardW + gap));

                graphics.fill(cardX, cardTop, cardX + cardW, cardTop + cardH, 0xF0181A22);
                graphics.outline(cardX, cardTop, cardW, cardH, 0xFF4A5060);

                graphics.fill(cardX + 1, cardTop + 1, cardX + cardW - 1, cardTop + 16, 0xF0252A38);
                String tTitle = "§eTeam " + t.teamIndex() + " §7(" + t.memberNames().size() + "/" + l.playersPerTeam() + ")";
                graphics.text(Minecraft.getInstance().font, Component.literal(tTitle), cardX + 4, cardTop + 4, 0xFFFFFFFF, false);

                int py = cardTop + 20;
                for (int slot = 0; slot < l.playersPerTeam(); slot++) {
                    if (slot < t.memberNames().size()) {
                        String member = t.memberNames().get(slot);
                        graphics.text(Minecraft.getInstance().font, Component.literal("§a• §f" + member), cardX + 6, py, 0xFFFFFFFF, false);
                    } else {
                        graphics.text(Minecraft.getInstance().font, Component.literal("§8- [Empty Slot]"), cardX + 6, py, 0xFF888888, false);
                    }
                    py += 11;
                    if (py > cardTop + cardH - 12) break;
                }
            }

            if (!l.spectatorNames().isEmpty()) {
                graphics.text(Minecraft.getInstance().font, Component.literal("§7Spectators: §f" + String.join(", ", l.spectatorNames())), contentX + 6, contentY + contentH - 90, 0xFFAAAAAA, false);
            }
        } else {
            graphics.text(Minecraft.getInstance().font, Component.literal("§6Active Public Lobbies:"), contentX + 6, contentY + 66, 0xFFFFFFFF, false);
            if (data.lobbies().isEmpty()) {
                graphics.text(Minecraft.getInstance().font, Component.literal("§7No active lobbies found. Create one above!"), contentX + 10, contentY + 80, 0xFFAAAAAA, false);
            }
        }
    }
}