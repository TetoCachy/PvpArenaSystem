package com.tetocachy.pvparenasystem.client.gui.tabs;

import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
import com.tetocachy.pvparenasystem.client.gui.ArenaScreenTab;
import com.tetocachy.pvparenasystem.network.C2SActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class AdminTab implements ArenaScreenTab {
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
        if (!ClientArenaCache.hasData()) return;
        boolean inSetup = ClientArenaCache.currentData.inSetup();

        if (!inSetup) {
            // Wand Toggle
            addWidget.accept(Button.builder(Component.literal("🪄 Toggle Wand"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_WAND", "", "", 0, 0));
            }).bounds(x + 6, y + 6, width - 12, 20).build());

            // Create from selection
            EditBox arenaNameBox = new EditBox(Minecraft.getInstance().font, x + 6, y + 36, 110, 18, Component.literal("Arena Name"));
            arenaNameBox.setHint(Component.literal("Arena Name..."));
            addWidget.accept(arenaNameBox);

            addWidget.accept(Button.builder(Component.literal("§aCreate & Setup"), b -> {
                if (!arenaNameBox.getValue().isBlank()) {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_CREATE_ARENA", arenaNameBox.getValue(), "", 0, 0));
                }
            }).bounds(x + 120, y + 35, width - 126, 20).build());
        } else {
            int half = (width - 16) / 2;
            addWidget.accept(Button.builder(Component.literal("🚩 Team 1"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SET_SPAWN", "", "", 1, 0));
            }).bounds(x + 6, y + 6, half, 20).build());

            addWidget.accept(Button.builder(Component.literal("🚩 Team 2"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SET_SPAWN", "", "", 2, 0));
            }).bounds(x + 10 + half, y + 6, half, 20).build());

            addWidget.accept(Button.builder(Component.literal("👁 Set Spectator"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SET_SPEC", "", "", 0, 0));
            }).bounds(x + 6, y + 30, width - 12, 20).build());

            addWidget.accept(Button.builder(Component.literal("§a💾 Save Arena"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SAVE_ARENA", "", "", 0, 0));
            }).bounds(x + 6, y + 54, half, 20).build());

            addWidget.accept(Button.builder(Component.literal("§c✕ Leave"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("ADMIN_LEAVE_SETUP", "", "", 0, 0));
            }).bounds(x + 10 + half, y + 54, half, 20).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
}