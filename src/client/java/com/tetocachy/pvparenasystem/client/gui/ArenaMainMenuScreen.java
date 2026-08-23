package com.tetocachy.pvparenasystem.client.gui;

import com.tetocachy.pvparenasystem.client.gui.tabs.*;
import com.tetocachy.pvparenasystem.network.C2SActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class ArenaMainMenuScreen extends Screen {
    private static final int WINDOW_WIDTH = 280;
    private static final int WINDOW_HEIGHT = 160;
    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 28;

    private int leftPos;
    private int topPos;
    private final List<ArenaScreenTab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;

    public ArenaMainMenuScreen() {
        super(Component.literal("PvP Arena System"));
        tabs.add(new DuelsTab());
        tabs.add(new PartyTab());
        tabs.add(new KitsTab());
        tabs.add(new ArenasTab());
        tabs.add(new AdminTab());
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - WINDOW_WIDTH) / 2;
        this.topPos = (this.height - WINDOW_HEIGHT) / 2 + 10;

        ClientPlayNetworking.send(new C2SActionPayload("REQUEST_SYNC", "", "", 0, 0));
        rebuildTabContent();
    }

    public void rebuildTabContent() {
        this.clearWidgets();

        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            ArenaScreenTab activeTab = tabs.get(activeTabIndex);
            int contentX = leftPos + 10;
            int contentY = topPos + 26;
            int contentWidth = WINDOW_WIDTH - 20;
            int contentHeight = WINDOW_HEIGHT - 34;
            activeTab.init(contentX, contentY, contentWidth, contentHeight, this::addRenderableWidget);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Handle clicking top advancement tabs
        int tabX = leftPos + 8;
        int tabY = topPos - 26;

        for (int i = 0; i < tabs.size(); i++) {
            ArenaScreenTab tab = tabs.get(i);
            if (!tab.isVisible()) continue;

            if (event.x() >= tabX && event.x() <= tabX + TAB_WIDTH && event.y() >= tabY && event.y() <= tabY + TAB_HEIGHT) {
                if (activeTabIndex != i) {
                    activeTabIndex = i;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    rebuildTabContent();
                }
                return true;
            }
            tabX += TAB_WIDTH + 4;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // 1. Draw Top Advancement Tabs
        int tabX = leftPos + 8;
        ArenaScreenTab hoveredTab = null;
        int hoveredTabX = 0;
        int hoveredTabY = 0;

        for (int i = 0; i < tabs.size(); i++) {
            ArenaScreenTab tab = tabs.get(i);
            if (!tab.isVisible()) continue;

            boolean isSelected = (i == activeTabIndex);
            int currentTabY = isSelected ? (topPos - 28) : (topPos - 24);
            int currentTabH = isSelected ? 30 : 25;

            // Tab Box Background
            int bgColor = isSelected ? 0xFF3C3C44 : 0xC0222228;
            int borderColor = isSelected ? 0xFFFFFFFF : 0xFF666670;

            graphics.fill(tabX, currentTabY, tabX + TAB_WIDTH, currentTabY + currentTabH, bgColor);
            graphics.outline(tabX, currentTabY, TAB_WIDTH, currentTabH, borderColor);

            if (isSelected) {
                // Blend bottom line into window
                graphics.fill(tabX + 1, topPos - 1, tabX + TAB_WIDTH - 1, topPos + 2, 0xFF3C3C44);
            }

            // Draw Tab Item Icon
            graphics.item(tab.getIcon(), tabX + 6, currentTabY + (isSelected ? 6 : 4));

            // Check Hover
            if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH && mouseY >= currentTabY && mouseY <= currentTabY + currentTabH) {
                hoveredTab = tab;
                hoveredTabX = tabX;
                hoveredTabY = currentTabY - 14;
            }

            tabX += TAB_WIDTH + 4;
        }

        // 2. Main Window Frame
        graphics.fill(leftPos, topPos, leftPos + WINDOW_WIDTH, topPos + WINDOW_HEIGHT, 0xF0181818);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + WINDOW_WIDTH - 2, topPos + WINDOW_HEIGHT - 2, 0xF02C2C34);
        graphics.outline(leftPos, topPos, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFFFFFFFF);

        // Header Separator
        graphics.fill(leftPos + 4, topPos + 20, leftPos + WINDOW_WIDTH - 4, topPos + 21, 0xFF4E4E58);

        // Header Title
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            ArenaScreenTab activeTab = tabs.get(activeTabIndex);
            graphics.text(this.font, activeTab.getTitle(), leftPos + 10, topPos + 7, 0xFFFFE680, true);
        }

        // 3. Render Tab Content
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).extractRenderState(graphics, mouseX, mouseY, partialTick);
        }

        // 4. Render Active Widgets (Buttons, Inputs)
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // 5. Draw Advancement-Style Tooltip when Hovering Tabs
        if (hoveredTab != null) {
            String text = hoveredTab.getTitle().getString();
            int textWidth = this.font.width(text);
            int tooltipX = Math.max(leftPos, hoveredTabX + (TAB_WIDTH / 2) - (textWidth / 2) - 4);
            int tooltipY = hoveredTabY;

            graphics.fill(tooltipX, tooltipY, tooltipX + textWidth + 8, tooltipY + 12, 0xF0101010);
            graphics.outline(tooltipX, tooltipY, textWidth + 8, 12, 0xFF888899);
            graphics.text(this.font, Component.literal(text), tooltipX + 4, tooltipY + 2, 0xFFFFFFFF, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}