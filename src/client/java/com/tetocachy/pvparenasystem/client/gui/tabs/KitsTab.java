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
    private int contentX, contentY, contentW, contentH;
    private int selectedKitIndex = 0;

    @Override
    public Component getTitle() { return Component.literal("PvP Kits & Loadouts"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.CHEST); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = width;
        this.contentH = height;

        if (!ClientArenaCache.hasData()) return;
        List<S2CSyncArenaDataPayload.KitInfo> kits = ClientArenaCache.currentData.kits();

        int btnY = y + 16;
        int listWidth = 110;

        for (int i = 0; i < Math.min(8, kits.size()); i++) {
            final int idx = i;
            S2CSyncArenaDataPayload.KitInfo k = kits.get(i);
            addWidget.accept(Button.builder(Component.literal("§b" + k.displayName()), b -> {
                selectedKitIndex = idx;
            }).bounds(x + 4, btnY, listWidth, 18).build());
            btnY += 20;
        }

        if (ClientArenaCache.currentData.isAdmin()) {
            EditBox nameBox = new EditBox(Minecraft.getInstance().font, x + 4, y + height - 22, 130, 18, Component.literal("Kit Name"));
            nameBox.setHint(Component.literal("Kit Name..."));
            addWidget.accept(nameBox);

            addWidget.accept(Button.builder(Component.literal("§aSave Gear"), b -> {
                if (!nameBox.getValue().isBlank()) {
                    ClientPlayNetworking.send(new C2SActionPayload("ADMIN_SAVE_KIT", nameBox.getValue(), "", 0, 0));
                    nameBox.setValue("");
                }
            }).bounds(x + 138, y + height - 23, width - 142, 20).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        List<S2CSyncArenaDataPayload.KitInfo> kits = ClientArenaCache.currentData.kits();

        graphics.text(Minecraft.getInstance().font, Component.literal("§6Saved Kits (" + kits.size() + "):"), contentX + 6, contentY + 4, 0xFFFFFFFF, false);

        int previewX = contentX + 120;
        int previewY = contentY + 12;
        int previewW = contentW - 124;
        int previewH = contentH - (ClientArenaCache.currentData.isAdmin() ? 40 : 18);

        graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xF0181A22);
        graphics.outline(previewX, previewY, previewW, previewH, 0xFF4A5060);

        if (!kits.isEmpty() && selectedKitIndex < kits.size()) {
            S2CSyncArenaDataPayload.KitInfo k = kits.get(selectedKitIndex);
            graphics.text(Minecraft.getInstance().font, Component.literal("§e" + k.displayName() + " Contents:"), previewX + 8, previewY + 6, 0xFFFFFFFF, false);

            int slotX = previewX + 8;
            int slotY = previewY + 20;
            int cols = (previewW - 16) / 20;

            for (int i = 0; i < k.previewItems().size(); i++) {
                ItemStack stack = k.previewItems().get(i);
                int sx = slotX + ((i % cols) * 20);
                int sy = slotY + ((i / cols) * 20);

                graphics.fill(sx, sy, sx + 18, sy + 18, 0xF0101116);
                graphics.outline(sx, sy, 18, 18, 0xFF353844);
                graphics.item(stack, sx + 1, sy + 1);

                if (stack.getCount() > 1) {
                    graphics.text(Minecraft.getInstance().font, Component.literal(String.valueOf(stack.getCount())), sx + 10, sy + 9, 0xFFFFFFFF, true);
                }
            }
        } else {
            graphics.text(Minecraft.getInstance().font, Component.literal("§7No kit selected."), previewX + 8, previewY + 20, 0xFFAAAAAA, false);
        }
    }
}