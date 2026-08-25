package com.tetocachy.pvparenasystem.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface ArenaScreenTab {
    Component getTitle();
    ItemStack getIcon();
    default boolean isVisible() {
        return true;
    }

    void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget);
    void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
    default void tick() {}
    default boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }
}