package com.tetocachy.pvparenasystem.client.gui.tabs;

import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
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

import java.util.List;
import java.util.function.Consumer;

public class LobbiesTab implements ArenaScreenTab {
    private int contentX, contentY, contentW, contentH;
    private int selectedArenaIndex = 0;
    private int selectedKitIndex = 0;
    private int teamCount = 2;
    private int playersPerTeam = 2;
    private int rounds = 3;

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

        if (data.currentLobby() != null) {
            // MMORPG LOBBY ROOM VIEW
            S2CSyncArenaDataPayload.LobbyInfo l = data.currentLobby();
            int teamsNum = Math.max(1, l.teams().size());
            int gap = 4;
            int cardW = (width - 8 - ((teamsNum - 1) * gap)) / teamsNum;

            for (int i = 0; i < l.teams().size(); i++) {
                final int teamIdx = i + 1;
                int cardX = x + 4 + (i * (cardW + gap));
                Button switchBtn = Button.builder(Component.literal("Join Team " + teamIdx), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_SWITCH_TEAM", "", "", teamIdx, 0));
                }).bounds(cardX, y + height - 48, cardW, 18).build();
                addWidget.accept(switchBtn);
            }

            // Spectator toggle button
            addWidget.accept(Button.builder(Component.literal("👁 Spectate"), b -> {
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
            // LOBBY BROWSER & CREATION
            boolean isPartyMemberNotLeader = data.party().inParty() && !data.party().isLeader();

            List<S2CSyncArenaDataPayload.ArenaInfo> arenas = data.arenas();
            String curArena = arenas.isEmpty() ? "Any Arena" : arenas.get(Math.min(selectedArenaIndex, arenas.size() - 1)).displayName();

            List<S2CSyncArenaDataPayload.KitInfo> kits = data.kits();
            String curKit = kits.isEmpty() ? "Default Kit" : kits.get(Math.min(selectedKitIndex, kits.size() - 1)).displayName();

            int colW = (width - 12) / 2;

            addWidget.accept(Button.builder(Component.literal("Map: §e" + curArena), b -> {
                if (!arenas.isEmpty()) {
                    selectedArenaIndex = (selectedArenaIndex + 1) % arenas.size();
                    b.setMessage(Component.literal("Map: §e" + arenas.get(selectedArenaIndex).displayName()));
                }
            }).bounds(x + 4, y + 2, colW, 18).build());

            addWidget.accept(Button.builder(Component.literal("Kit: §b" + curKit), b -> {
                if (!kits.isEmpty()) {
                    selectedKitIndex = (selectedKitIndex + 1) % kits.size();
                    b.setMessage(Component.literal("Kit: §b" + kits.get(selectedKitIndex).displayName()));
                }
            }).bounds(x + colW + 8, y + 2, colW, 18).build());

            int thirdW = (width - 16) / 3;

            addWidget.accept(Button.builder(Component.literal("Teams: §a" + teamCount), b -> {
                teamCount = teamCount >= 8 ? 2 : teamCount + 1;
                b.setMessage(Component.literal("Teams: §a" + teamCount));
            }).bounds(x + 4, y + 22, thirdW, 18).build());

            addWidget.accept(Button.builder(Component.literal("Per Team: §a" + playersPerTeam), b -> {
                playersPerTeam = playersPerTeam >= 10 ? 1 : (playersPerTeam == 1 ? 2 : (playersPerTeam == 2 ? 4 : (playersPerTeam == 4 ? 5 : 10)));
                b.setMessage(Component.literal("Per Team: §a" + playersPerTeam));
            }).bounds(x + thirdW + 8, y + 22, thirdW, 18).build());

            addWidget.accept(Button.builder(Component.literal("Rounds: §eBo" + (rounds * 2 - 1)), b -> {
                rounds = rounds >= 4 ? 1 : rounds + 1;
                b.setMessage(Component.literal("Rounds: §eBo" + (rounds * 2 - 1)));
            }).bounds(x + (thirdW * 2) + 12, y + 22, thirdW, 18).build());

            if (isPartyMemberNotLeader) {
                Button lockedCreate = Button.builder(Component.literal("§c🔒 Only Party Leader Can Host"), b -> {}).bounds(x + 4, y + 43, width - 8, 20).build();
                lockedCreate.active = false;
                addWidget.accept(lockedCreate);
            } else {
                addWidget.accept(Button.builder(Component.literal("§a§l+ CREATE & OPEN LOBBY"), b -> {
                    String aId = arenas.isEmpty() ? "" : arenas.get(Math.min(selectedArenaIndex, arenas.size() - 1)).id();
                    String kId = kits.isEmpty() ? "" : kits.get(Math.min(selectedKitIndex, kits.size() - 1)).id();
                    ClientPlayNetworking.send(new C2SActionPayload("LOBBY_CREATE", aId, kId, teamCount, playersPerTeam, rounds));
                }).bounds(x + 4, y + 43, width - 8, 20).build());
            }

            // List of Public Lobbies
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

                if (isPartyMemberNotLeader) {
                    Button lockedJoin = Button.builder(Component.literal("§7" + l.hostName() + "'s Lobby §c[Locked: In Party]"), b -> {}).bounds(x + 4, listY, width - 8, 18).build();
                    lockedJoin.active = false;
                    addWidget.accept(lockedJoin);
                } else {
                    String label = "§e" + l.hostName() + " §7[" + l.arenaName() + "] §b(" + totalInLobby + "/" + maxCapacity + ")";
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

            // Ongoing Live Matches Section
            if (!data.activeMatches().isEmpty()) {
                listY += 4;
                for (S2CSyncArenaDataPayload.OngoingMatchInfo match : data.activeMatches()) {
                    String mLabel = "§6⚔ " + match.arenaName() + " §7(Round " + match.currentRound() + "/" + match.totalRounds() + ") §b[" + match.playerCount() + " Fighters]";
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload data = ClientArenaCache.currentData;

        if (data.currentLobby() != null) {
            S2CSyncArenaDataPayload.LobbyInfo l = data.currentLobby();
            int teamsNum = Math.max(1, l.teams().size());
            int gap = 4;
            int cardW = (contentW - 8 - ((teamsNum - 1) * gap)) / teamsNum;

            graphics.fill(contentX + 4, contentY + 2, contentX + contentW - 4, contentY + 20, 0xF0161820);
            graphics.outline(contentX + 4, contentY + 2, contentW - 8, 18, 0xFF4A4E5C);

            String header = "§6Host: §f" + l.hostName() + "  §7|  §eMap: §f" + l.arenaName() + "  §7|  §bKit: §f" + l.kitName() + "  §7|  §aBest of " + (l.rounds() * 2 - 1);
            graphics.text(Minecraft.getInstance().font, Component.literal(header), contentX + 8, contentY + 7, 0xFFFFFFFF, false);

            int cardTop = contentY + 24;
            int cardH = contentH - 98;

            for (int i = 0; i < l.teams().size(); i++) {
                S2CSyncArenaDataPayload.TeamSlotInfo t = l.teams().get(i);
                int cardX = contentX + 4 + (i * (cardW + gap));

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