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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public class AdminTab implements ArenaScreenTab {
    private enum AdminSubView {
        MAP_EDITOR,
        MATCH_SETTINGS
    }

    private final ArenaMainMenuScreen parentScreen;
    private AdminSubView currentView = AdminSubView.MAP_EDITOR;
    private int contentX, contentY, contentW, contentH;
    private int selectedEditArenaIndex = 0;
    private int currentSetupTeam = 1;

    public AdminTab(ArenaMainMenuScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public Component getTitle() { return Component.literal("Admin Tools & Settings"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.COMMAND_BLOCK); }

    @Override
    public boolean isVisible() {
        return ClientArenaCache.hasData() && ClientArenaCache.currentData.isAdmin();
    }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = width;
        this.contentH = height;

        if (!ClientArenaCache.hasData()) return;
        boolean inSetup = ClientArenaCache.currentData.inSetup();
        S2CSyncArenaDataPayload data = ClientArenaCache.currentData;
        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = data.arenas();

        int pad = 4;
        int usableW = width - (pad * 2);

        if (inSetup) {
            // Setup Mode Controls
            int cardPad = 8;
            int innerW = usableW - (cardPad * 2);
            int halfW = (innerW - pad) / 2;

            int navBtnW = 22;
            int teamBtnW = innerW - (navBtnW * 2) - (pad * 2);

            addWidget.accept(Button.builder(Component.literal("◀"), b -> {
                if (currentSetupTeam > 1) {
                    currentSetupTeam--;
                }
            }).bounds(x + pad + cardPad, y + 26, navBtnW, 20).build());

            Button setTeamBtn = Button.builder(Component.literal("🚩 Set Spawn for Team " + currentSetupTeam), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SET_SPAWN", "", "", currentSetupTeam, 0));
            }).bounds(x + pad + cardPad + navBtnW + pad, y + 26, teamBtnW, 20).build();
            addWidget.accept(setTeamBtn);

            addWidget.accept(Button.builder(Component.literal("▶"), b -> {
                currentSetupTeam++;
                setTeamBtn.setMessage(Component.literal("🚩 Set Spawn for Team " + currentSetupTeam));
            }).bounds(x + pad + cardPad + navBtnW + pad + teamBtnW + pad, y + 26, navBtnW, 20).build());

            addWidget.accept(Button.builder(Component.literal("👁 Set Spectator Spawn"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SET_SPEC", "", "", 0, 0));
            }).bounds(x + pad + cardPad, y + 52, innerW, 20).build());

            int bottomY = y + height - 26;
            addWidget.accept(Button.builder(Component.literal("§a💾 Save Arena"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SAVE_ARENA", "", "", 0, 0));
            }).bounds(x + pad + cardPad, bottomY, halfW, 22).build());

            addWidget.accept(Button.builder(Component.literal("§c✕ Exit Setup"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_LEAVE_SETUP", "", "", 0, 0));
            }).bounds(x + pad + cardPad + halfW + pad, bottomY, halfW, 22).build());

            return;
        }

        // Sub-Menu Navigation Buttons (Top bar)
        int navW = (usableW - pad) / 2;
        String mapLabel = (currentView == AdminSubView.MAP_EDITOR ? "§6§l▶ " : "") + "🗺 Map & Arena Editor";
        String settingsLabel = (currentView == AdminSubView.MATCH_SETTINGS ? "§6§l▶ " : "") + "⚙ Match & Lobby Settings";

        addWidget.accept(Button.builder(Component.literal(mapLabel), b -> {
            currentView = AdminSubView.MAP_EDITOR;
            parentScreen.rebuildTabContent();
        }).bounds(x + pad, y + 2, navW, 20).build());

        addWidget.accept(Button.builder(Component.literal(settingsLabel), b -> {
            currentView = AdminSubView.MATCH_SETTINGS;
            parentScreen.rebuildTabContent();
        }).bounds(x + pad + navW + pad, y + 2, navW, 20).build());

        int panelY = y + 26;

        if (currentView == AdminSubView.MAP_EDITOR) {
            // --- SECTION 1: MAP & ARENA EDITOR ---
            int wandW = 90;
            int btnW = 100;
            int inputW = usableW - wandW - btnW - (pad * 2);

            addWidget.accept(Button.builder(Component.literal("🪄 Wand"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_WAND", "", "", 0, 0));
            }).bounds(x + pad, panelY + 6, wandW, 20).build());

            EditBox arenaNameBox = new EditBox(Minecraft.getInstance().font, x + pad + wandW + pad, panelY + 6, inputW, 20, Component.literal("Arena Name"));
            arenaNameBox.setHint(Component.literal("New Arena Name..."));
            addWidget.accept(arenaNameBox);

            addWidget.accept(Button.builder(Component.literal("§a+ Create"), b -> {
                if (!arenaNameBox.getValue().isBlank()) {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_CREATE_ARENA", arenaNameBox.getValue().trim(), "", 0, 0));
                }
            }).bounds(x + pad + wandW + pad + inputW + pad, panelY + 6, btnW, 20).build());

            if (!arenas.isEmpty()) {
                if (selectedEditArenaIndex >= arenas.size()) selectedEditArenaIndex = 0;
                S2CSyncArenaDataPayload.ArenaInfo sel = arenas.get(selectedEditArenaIndex);

                int cardY = panelY + 34;
                int editBtnW = 110;
                int selectorW = usableW - editBtnW - pad;

                addWidget.accept(Button.builder(Component.literal("Map: §e" + sel.displayName() + " §7(" + (selectedEditArenaIndex + 1) + "/" + arenas.size() + ")"), b -> {
                    selectedEditArenaIndex = (selectedEditArenaIndex + 1) % arenas.size();
                    S2CSyncArenaDataPayload.ArenaInfo next = arenas.get(selectedEditArenaIndex);
                    b.setMessage(Component.literal("Map: §e" + next.displayName() + " §7(" + (selectedEditArenaIndex + 1) + "/" + arenas.size() + ")"));
                }).bounds(x + pad, cardY, selectorW, 18).build());

                addWidget.accept(Button.builder(Component.literal("§e✏ Setup Mode"), b -> {
                    S2CSyncArenaDataPayload.ArenaInfo target = arenas.get(selectedEditArenaIndex);
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_EDIT_ARENA", target.id(), "", 0, 0));
                }).bounds(x + pad + selectorW + pad, cardY, editBtnW, 18).build());
            }

        } else {
            // --- SECTION 2: MATCH & LOBBY SETTINGS ---
            int colW = (usableW - (pad * 4)) / 3;

            // Column 1: Team Sizes (XvX)
            int col1X = x + pad;
            EditBox sizeBox = new EditBox(Minecraft.getInstance().font, col1X, panelY + 16, colW - 36, 18, Component.literal("Size"));
            sizeBox.setHint(Component.literal("e.g. 100"));
            addWidget.accept(sizeBox);

            addWidget.accept(Button.builder(Component.literal("+"), b -> {
                try {
                    int val = Integer.parseInt(sizeBox.getValue().trim());
                    if (val > 0) {
                        ClientPlayNetworking.send(new C2SActionPayload("ADMIN_ADD_PRESET", "TEAM_SIZE", "", val, 0));
                        sizeBox.setValue("");
                    }
                } catch (Exception ignored) {}
            }).bounds(col1X + colW - 34, panelY + 16, 34, 18).build());

            int sy = panelY + 38;
            for (int s : data.allowedTeamSizes()) {
                addWidget.accept(Button.builder(Component.literal(s + "v" + s + " §c✕"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_REMOVE_PRESET", "TEAM_SIZE", "", s, 0));
                }).bounds(col1X, sy, colW, 15).build());
                sy += 16;
                if (sy > y + height - 16) break;
            }

            // Column 2: Team Counts
            int col2X = col1X + colW + (pad * 2);
            EditBox countBox = new EditBox(Minecraft.getInstance().font, col2X, panelY + 16, colW - 36, 18, Component.literal("Count"));
            countBox.setHint(Component.literal("e.g. 16"));
            addWidget.accept(countBox);

            addWidget.accept(Button.builder(Component.literal("+"), b -> {
                try {
                    int val = Integer.parseInt(countBox.getValue().trim());
                    if (val >= 2) {
                        ClientPlayNetworking.send(new C2SActionPayload("ADMIN_ADD_PRESET", "TEAM_COUNT", "", val, 0));
                        countBox.setValue("");
                    }
                } catch (Exception ignored) {}
            }).bounds(col2X + colW - 34, panelY + 16, 34, 18).build());

            int cy = panelY + 38;
            for (int c : data.allowedTeamCounts()) {
                addWidget.accept(Button.builder(Component.literal(c + " Teams §c✕"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_REMOVE_PRESET", "TEAM_COUNT", "", c, 0));
                }).bounds(col2X, cy, colW, 15).build());
                cy += 16;
                if (cy > y + height - 16) break;
            }

            // Column 3: Goal Points
            int col3X = col2X + colW + (pad * 2);
            EditBox goalBox = new EditBox(Minecraft.getInstance().font, col3X, panelY + 16, colW - 36, 18, Component.literal("Points"));
            goalBox.setHint(Component.literal("e.g. 50"));
            addWidget.accept(goalBox);

            addWidget.accept(Button.builder(Component.literal("+"), b -> {
                try {
                    int val = Integer.parseInt(goalBox.getValue().trim());
                    if (val > 0) {
                        ClientPlayNetworking.send(new C2SActionPayload("ADMIN_ADD_PRESET", "GOAL_POINT", "", val, 0));
                        goalBox.setValue("");
                    }
                } catch (Exception ignored) {}
            }).bounds(col3X + colW - 34, panelY + 16, 34, 18).build());

            int gy = panelY + 38;
            for (int p : data.allowedGoalPoints()) {
                String unit = p == 1 ? "Pt" : "Pts";
                addWidget.accept(Button.builder(Component.literal(p + " " + unit + " §c✕"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_REMOVE_PRESET", "GOAL_POINT", "", p, 0));
                }).bounds(col3X, gy, colW, 15).build());
                gy += 16;
                if (gy > y + height - 16) break;
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        boolean inSetup = ClientArenaCache.currentData.inSetup();
        S2CSyncArenaDataPayload data = ClientArenaCache.currentData;
        int pad = 4;
        int usableW = contentW - (pad * 2);

        if (inSetup) {
            int cardH = contentH - 8;
            graphics.fill(contentX + pad, contentY + pad, contentX + contentW - pad, contentY + pad + cardH, 0xF0161820);
            graphics.outline(contentX + pad, contentY + pad, usableW, cardH, 0xFF555C70);

            String editingId = data.editingArenaId();
            graphics.text(Minecraft.getInstance().font, Component.literal("§6§lArena Setup Mode: §e" + (editingId.isBlank() ? "Active Session" : editingId)), contentX + pad + 8, contentY + pad + 6, 0xFFFFFFFF, false);

            int infoY = contentY + 78;
            graphics.text(Minecraft.getInstance().font, Component.literal("§eSpawn Configuration Instructions:"), contentX + pad + 8, infoY, 0xFFFFFF88, false);
            infoY += 12;
            graphics.text(Minecraft.getInstance().font, Component.literal("§7• Stand at a team position and click §fSet Spawn for Team X§7."), contentX + pad + 12, infoY, 0xFFCCCCCC, false);
            infoY += 11;
            graphics.text(Minecraft.getInstance().font, Component.literal("§7• Use ◀ / ▶ to configure Team 1, Team 2, Team 3, etc."), contentX + pad + 12, infoY, 0xFFCCCCCC, false);
            infoY += 11;
            graphics.text(Minecraft.getInstance().font, Component.literal("§7• All configured spawns are highlighted in the world with team colors."), contentX + pad + 12, infoY, 0xFFCCCCCC, false);
            return;
        }

        int panelY = contentY + 26;
        int panelH = contentH - 28;

        graphics.fill(contentX + pad, panelY, contentX + contentW - pad, panelY + panelH, 0xF0181A22);
        graphics.outline(contentX + pad, panelY, usableW, panelH, 0xFF4A5060);

        if (currentView == AdminSubView.MAP_EDITOR) {
            List<S2CSyncArenaDataPayload.ArenaInfo> arenas = data.arenas();
            if (arenas.isEmpty()) {
                graphics.text(Minecraft.getInstance().font, Component.literal("§7No registered arenas found. Select a region with the wand and create one above!"), contentX + pad + 8, panelY + 60, 0xFFAAAAAA, false);
            } else if (selectedEditArenaIndex < arenas.size()) {
                S2CSyncArenaDataPayload.ArenaInfo sel = arenas.get(selectedEditArenaIndex);
                String statusBadge = sel.status() == 2 ? "§c[IN USE]" : (sel.status() == 1 ? "§a[READY]" : "§e[SETUP NEEDED]");

                int textY = panelY + 60;
                graphics.text(Minecraft.getInstance().font, Component.literal("§7ID: §f" + sel.id() + "  |  Status: " + statusBadge), contentX + pad + 8, textY, 0xFFFFFFFF, false);
                textY += 12;
                graphics.text(Minecraft.getInstance().font, Component.literal("§7Team Spawns: §b" + sel.teamSpawnCount() + " configured  §7|  Spectator: " + (sel.hasSpectatorSpawn() ? "§a✔ Configured" : "§c✖ Missing")), contentX + pad + 8, textY, 0xFFEEEEEE, false);
                textY += 12;
                graphics.text(Minecraft.getInstance().font, Component.literal("§7Dimensions: §f" + sel.sizeX() + "x" + sel.sizeY() + "x" + sel.sizeZ() + " blocks  §7|  Border: §e" + sel.borderShape()), contentX + pad + 8, textY, 0xFFCCCCCC, false);
            }
        } else {
            int colW = (usableW - (pad * 4)) / 3;
            int col1X = contentX + pad + 4;
            int col2X = col1X + colW + (pad * 2);
            int col3X = col2X + colW + (pad * 2);

            graphics.text(Minecraft.getInstance().font, Component.literal("§eTeam Sizes:"), col1X, panelY + 4, 0xFFFFFFFF, false);
            graphics.text(Minecraft.getInstance().font, Component.literal("§eTeam Counts:"), col2X, panelY + 4, 0xFFFFFFFF, false);
            graphics.text(Minecraft.getInstance().font, Component.literal("§eGoal Points:"), col3X, panelY + 4, 0xFFFFFFFF, false);
        }
    }
}