package com.tetocachy.pvparenasystem.client.gui;

import com.tetocachy.pvparenasystem.network.C2SActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SpawnSelectorScreen extends Screen {
    public SpawnSelectorScreen() {
        super(Component.literal("Spawn Selector"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - 80;
        int cy = this.height / 2 - 40;

        this.addRenderableWidget(Button.builder(Component.literal("🚩 Team 1 Spawn"), b -> {
            ClientPlayNetworking.send(new C2SActionPayload("SET_SPAWN_AT_BLOCK", "", "", 1, 0));
            this.onClose();
        }).bounds(cx, cy, 160, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("🚩 Team 2 Spawn"), b -> {
            ClientPlayNetworking.send(new C2SActionPayload("SET_SPAWN_AT_BLOCK", "", "", 2, 0));
            this.onClose();
        }).bounds(cx, cy + 24, 160, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("👁 Spectator Spawn"), b -> {
            ClientPlayNetworking.send(new C2SActionPayload("SET_SPAWN_AT_BLOCK", "", "", 99, 0));
            this.onClose();
        }).bounds(cx, cy + 48, 160, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2 - 90;
        int cy = this.height / 2 - 50;
        graphics.fill(cx, cy, cx + 180, cy + 90, 0xF0181822);
        graphics.outline(cx, cy, 180, 90, 0xFFFFFFFF);
        graphics.text(this.font, Component.literal("§6Set Spawn at Block:"), cx + 10, cy + 8, 0xFFFFFFFF, true);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}