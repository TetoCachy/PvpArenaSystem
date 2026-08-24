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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public class AdminTab implements ArenaScreenTab {
    private int contentX, contentY, contentW, contentH;
    private int selectedEditArenaIndex = 0;
    private int currentSetupTeam = 1;

    @Override
    public Component getTitle() { return Component.literal("Admin Tools"); }
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
        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = ClientArenaCache.currentData.arenas();

        int pad = 4;
        int usableW = width - (pad * 2);

        if (!inSetup) {
            // Row 1: Wand Toggle
            addWidget.accept(Button.builder(Component.literal("🪄 Toggle Selection Wand"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_WAND", "", "", 0, 0));
            }).bounds(x + pad, y + pad, usableW, 20).build());

            // Row 2: Create New Arena from Wand Selection
            int btnW = 120;
            int inputW = usableW - btnW - pad;

            EditBox arenaNameBox = new EditBox(Minecraft.getInstance().font, x + pad, y + 28, inputW, 20, Component.literal("Arena Name"));
            arenaNameBox.setHint(Component.literal("New Arena Name..."));
            addWidget.accept(arenaNameBox);

            addWidget.accept(Button.builder(Component.literal("§a+ Create & Setup"), b -> {
                if (!arenaNameBox.getValue().isBlank()) {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_CREATE_ARENA", arenaNameBox.getValue().trim(), "", 0, 0));
                }
            }).bounds(x + pad + inputW + pad, y + 28, btnW, 20).build());

            // Row 3: Modify Existing Arenas Card
            if (!arenas.isEmpty()) {
                if (selectedEditArenaIndex >= arenas.size()) selectedEditArenaIndex = 0;
                S2CSyncArenaDataPayload.ArenaInfo sel = arenas.get(selectedEditArenaIndex);

                int cardY = y + 74;
                int editBtnW = 110;
                int selectorW = usableW - editBtnW - pad - 12;

                // Cycle Map Button
                addWidget.accept(Button.builder(Component.literal("Map: §e" + sel.displayName() + " §7(" + (selectedEditArenaIndex + 1) + "/" + arenas.size() + ")"), b -> {
                    selectedEditArenaIndex = (selectedEditArenaIndex + 1) % arenas.size();
                    S2CSyncArenaDataPayload.ArenaInfo next = arenas.get(selectedEditArenaIndex);
                    b.setMessage(Component.literal("Map: §e" + next.displayName() + " §7(" + (selectedEditArenaIndex + 1) + "/" + arenas.size() + ")"));
                }).bounds(x + pad + 6, cardY, selectorW, 20).build());

                // Edit in Setup Button
                addWidget.accept(Button.builder(Component.literal("§e✏ Edit in Setup"), b -> {
                    S2CSyncArenaDataPayload.ArenaInfo target = arenas.get(selectedEditArenaIndex);
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_EDIT_ARENA", target.id(), "", 0, 0));
                }).bounds(x + pad + 6 + selectorW + pad, cardY, editBtnW, 20).build());
            }

        } else {
            // Setup Mode Controls
            int cardPad = 8;
            int innerW = usableW - (cardPad * 2);
            int halfW = (innerW - pad) / 2;

            // Row 1: Infinite Team Spawn Selector
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

            // Row 2: Spectator & Lobby Spawns
            addWidget.accept(Button.builder(Component.literal("👁 Set Spectator Spawn"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SET_SPEC", "", "", 0, 0));
            }).bounds(x + pad + cardPad, y + 52, halfW, 20).build());

            addWidget.accept(Button.builder(Component.literal("🏠 Set Lobby Spawn"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("SET_SPAWN_AT_BLOCK", "", "", 100, 0));
            }).bounds(x + pad + cardPad + halfW + pad, y + 52, halfW, 20).build());

            // Bottom Action Row: Save & Exit
            int bottomY = y + height - 26;
            addWidget.accept(Button.builder(Component.literal("§a💾 Save Arena"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SAVE_ARENA", "", "", 0, 0));
            }).bounds(x + pad + cardPad, bottomY, halfW, 22).build());

            addWidget.accept(Button.builder(Component.literal("§c✕ Exit Setup"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_LEAVE_SETUP", "", "", 0, 0));
            }).bounds(x + pad + cardPad + halfW + pad, bottomY, halfW, 22).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        boolean inSetup = ClientArenaCache.currentData.inSetup();
        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = ClientArenaCache.currentData.arenas();

        int pad = 4;

        if (!inSetup) {
            int cardY = contentY + 54;
            int cardH = contentH - 58;

            graphics.fill(contentX + pad, cardY, contentX + contentW - pad, cardY + cardH, 0xF0181A22);
            graphics.outline(contentX + pad, cardY, contentW - (pad * 2), cardH, 0xFF4A5060);

            graphics.text(Minecraft.getInstance().font, Component.literal("§6Modify Existing Arenas:"), contentX + pad + 6, cardY + 6, 0xFFFFFFFF, false);

            if (arenas.isEmpty()) {
                graphics.text(Minecraft.getInstance().font, Component.literal("§7No registered arenas found. Create one above!"), contentX + pad + 8, cardY + 24, 0xFFAAAAAA, false);
            } else if (selectedEditArenaIndex < arenas.size()) {
                S2CSyncArenaDataPayload.ArenaInfo sel = arenas.get(selectedEditArenaIndex);
                String statusBadge = sel.status() == 2 ? "§c[IN USE]" : (sel.status() == 1 ? "§a[READY]" : "§e[SETUP NEEDED]");

                int textY = cardY + 46;
                graphics.text(Minecraft.getInstance().font, Component.literal("§7ID: §f" + sel.id() + "  |  Status: " + statusBadge), contentX + pad + 8, textY, 0xFFFFFFFF, false);
                textY += 12;
                graphics.text(Minecraft.getInstance().font, Component.literal("§7Team Spawns: §b" + sel.teamSpawnCount() + " configured  §7|  Spectator: " + (sel.hasSpectatorSpawn() ? "§a✔ Configured" : "§c✖ Missing")), contentX + pad + 8, textY, 0xFFEEEEEE, false);
                textY += 12;
                graphics.text(Minecraft.getInstance().font, Component.literal("§7Dimensions: §f" + sel.sizeX() + "x" + sel.sizeY() + "x" + sel.sizeZ() + " blocks  §7|  Border: §e" + sel.borderShape()), contentX + pad + 8, textY, 0xFFCCCCCC, false);
            }
        } else {
            int cardH = contentH - 8;
            graphics.fill(contentX + pad, contentY + pad, contentX + contentW - pad, contentY + pad + cardH, 0xF0161820);
            graphics.outline(contentX + pad, contentY + pad, contentW - (pad * 2), cardH, 0xFF555C70);

            String editingId = ClientArenaCache.currentData.editingArenaId();
            graphics.text(Minecraft.getInstance().font, Component.literal("§6§lArena Setup Mode: §e" + (editingId.isBlank() ? "Active Session" : editingId)), contentX + pad + 8, contentY + pad + 6, 0xFFFFFFFF, false);

            int infoY = contentY + 78;
            graphics.text(Minecraft.getInstance().font, Component.literal("§eSpawn Configuration Instructions:"), contentX + pad + 8, infoY, 0xFFFFFF88, false);
            infoY += 12;
            graphics.text(Minecraft.getInstance().font, Component.literal("§7• Stand at a team position and click §fSet Spawn for Team X§7."), contentX + pad + 12, infoY, 0xFFCCCCCC, false);
            infoY += 11;
            graphics.text(Minecraft.getInstance().font, Component.literal("§7• Use ◀ / ▶ to configure Team 1, Team 2, Team 3, etc."), contentX + pad + 12, infoY, 0xFFCCCCCC, false);
            infoY += 11;
            graphics.text(Minecraft.getInstance().font, Component.literal("§7• All configured spawns are highlighted in the world with team colors."), contentX + pad + 12, infoY, 0xFFCCCCCC, false);
        }
    }
}