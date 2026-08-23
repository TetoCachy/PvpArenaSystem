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

public class KitsTab implements ArenaScreenTab {
    private int contentX, contentY;

    @Override
    public Component getTitle() { return Component.literal("PvP Kits"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.CHEST); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        if (!ClientArenaCache.hasData()) return;

        if (ClientArenaCache.currentData.isAdmin()) {
            EditBox nameBox = new EditBox(Minecraft.getInstance().font, x + 6, y + height - 24, 110, 18, Component.literal("Kit Name"));
            nameBox.setHint(Component.literal("Kit Name..."));
            addWidget.accept(nameBox);

            addWidget.accept(Button.builder(Component.literal("§aSave Gear"), b -> {
                if (!nameBox.getValue().isBlank()) {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SAVE_KIT", nameBox.getValue(), "", 0, 0));
                    nameBox.setValue("");
                }
            }).bounds(x + 120, y + height - 25, width - 126, 20).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        List<S2CSyncArenaDataPayload.KitInfo> kits = ClientArenaCache.currentData.kits();

        int drawY = contentY + 6;
        graphics.text(Minecraft.getInstance().font, Component.literal("§6Available Kits (" + kits.size() + "):"), contentX + 6, drawY, 0xFFFFFFFF, false);
        drawY += 14;

        if (kits.isEmpty()) {
            graphics.text(Minecraft.getInstance().font, Component.literal("§7No custom kits saved yet."), contentX + 12, drawY, 0xFFAAAAAA, false);
        } else {
            for (int i = 0; i < Math.min(5, kits.size()); i++) {
                S2CSyncArenaDataPayload.KitInfo k = kits.get(i);
                graphics.text(Minecraft.getInstance().font, Component.literal("• §b" + k.displayName()), contentX + 12, drawY, 0xFFDDDDDD, false);
                drawY += 12;
            }
        }
    }
}