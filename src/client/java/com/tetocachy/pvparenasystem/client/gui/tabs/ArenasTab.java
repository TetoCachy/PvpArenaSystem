package com.tetocachy.pvparenasystem.client.gui.tabs;

import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
import com.tetocachy.pvparenasystem.client.gui.ArenaScreenTab;
import com.tetocachy.pvparenasystem.network.S2CSyncArenaDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public class ArenasTab implements ArenaScreenTab {
    private int contentX, contentY;

    @Override
    public Component getTitle() { return Component.literal("Arenas & Maps"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.COMPASS); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = ClientArenaCache.currentData.arenas();

        int drawY = contentY + 6;
        graphics.text(Minecraft.getInstance().font, Component.literal("§6Registered Maps (" + arenas.size() + "):"), contentX + 6, drawY, 0xFFFFFFFF, false);
        drawY += 14;

        if (arenas.isEmpty()) {
            graphics.text(Minecraft.getInstance().font, Component.literal("§7No arenas created yet."), contentX + 12, drawY, 0xFFAAAAAA, false);
        } else {
            for (int i = 0; i < Math.min(5, arenas.size()); i++) {
                S2CSyncArenaDataPayload.ArenaInfo a = arenas.get(i);
                String badge = a.status() == 2 ? "§c[IN USE]" : (a.status() == 1 ? "§a[READY]" : "§e[SETUP]");
                graphics.text(Minecraft.getInstance().font, Component.literal("• §f" + a.displayName() + " " + badge), contentX + 12, drawY, 0xFFEEEEEE, false);
                drawY += 12;
            }
        }
    }
}