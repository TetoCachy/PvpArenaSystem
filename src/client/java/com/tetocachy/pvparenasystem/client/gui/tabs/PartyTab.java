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

import java.util.function.Consumer;

public class PartyTab implements ArenaScreenTab {
    private int contentX, contentY;

    @Override
    public Component getTitle() { return Component.literal("Party Management"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.SHIELD); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload.PartyInfo party = ClientArenaCache.currentData.party();

        if (!party.inParty()) {
            addWidget.accept(Button.builder(Component.literal("§a+ Create New Party"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("PARTY_CREATE", "", "", 0, 0));
            }).bounds(x + 30, y + 45, width - 60, 22).build());
        } else {
            addWidget.accept(Button.builder(Component.literal("§cLeave Party"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("PARTY_LEAVE", "", "", 0, 0));
            }).bounds(x + 30, y + height - 26, width - 60, 20).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload.PartyInfo party = ClientArenaCache.currentData.party();

        if (party.inParty()) {
            graphics.text(Minecraft.getInstance().font, Component.literal("§eParty Leader: §f" + party.leaderName()), contentX + 10, contentY + 8, 0xFFFFFFFF, false);
            graphics.text(Minecraft.getInstance().font, Component.literal("§7Members: §b" + String.join(", ", party.members())), contentX + 10, contentY + 24, 0xFFDDDDDD, false);
        } else {
            graphics.text(Minecraft.getInstance().font, Component.literal("§7You are not currently in a party."), contentX + 10, contentY + 15, 0xFFAAAAAA, false);
        }
    }
}