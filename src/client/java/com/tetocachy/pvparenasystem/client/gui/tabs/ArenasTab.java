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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ArenasTab implements ArenaScreenTab {
    private int contentX, contentY, contentW, contentH;

    @Override
    public Component getTitle() { return Component.literal("Arenas & Maps"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.COMPASS); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = width;
        this.contentH = height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        List<S2CSyncArenaDataPayload.ArenaInfo> arenas = ClientArenaCache.currentData.arenas();

        graphics.text(Minecraft.getInstance().font, Component.literal("§6Registered Maps (" + arenas.size() + "):"), contentX + 4, contentY + 2, 0xFFFFFFFF, false);

        if (arenas.isEmpty()) {
            graphics.text(Minecraft.getInstance().font, Component.literal("§7No arenas created yet. Create one in the Admin Tab!"), contentX + 8, contentY + 20, 0xFFAAAAAA, false);
            return;
        }

        int cardY = contentY + 16;
        int cardH = 26;
        S2CSyncArenaDataPayload.ArenaInfo hoveredArena = null;

        for (S2CSyncArenaDataPayload.ArenaInfo a : arenas) {
            boolean isHovered = mouseX >= contentX + 4 && mouseX <= contentX + contentW - 4 && mouseY >= cardY && mouseY <= cardY + cardH;
            if (isHovered) hoveredArena = a;

            int bgColor = isHovered ? 0xF0252A38 : 0xF0181A22;
            int borderColor = isHovered ? 0xFF6A7590 : 0xFF3E4354;

            graphics.fill(contentX + 4, cardY, contentX + contentW - 4, cardY + cardH, bgColor);
            graphics.outline(contentX + 4, cardY, contentW - 8, cardH, borderColor);

            String statusBadge = a.status() == 2 ? "§c[IN USE]" : (a.status() == 1 ? "§a[READY]" : "§e[SETUP NEEDED]");
            graphics.text(Minecraft.getInstance().font, Component.literal("§f" + a.displayName() + " " + statusBadge), contentX + 8, cardY + 4, 0xFFFFFFFF, false);

            String teamInfo = a.teamSpawnCount() >= 2
                    ? "§7Supports: §b2 to " + a.teamSpawnCount() + " Teams §7(Up to " + a.maxPlayersPerTeam() + "/team)"
                    : "§c⚠ Missing team spawns (" + a.teamSpawnCount() + "/2 configured)";
            graphics.text(Minecraft.getInstance().font, Component.literal(teamInfo), contentX + 8, cardY + 15, 0xFFAAAAAA, false);

            cardY += cardH + 4;
            if (cardY > contentY + contentH - 28) break;
        }

        // Render Hover Tooltip
        if (hoveredArena != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6§l" + hoveredArena.displayName()));
            tooltip.add(Component.literal("§7ID: §f" + hoveredArena.id()));
            tooltip.add(Component.literal("§7Status: " + (hoveredArena.status() == 2 ? "§cIn Match" : (hoveredArena.status() == 1 ? "§aReady for Duels" : "§eIn Setup"))));
            tooltip.add(Component.literal("§7Team Capacity: §b" + (hoveredArena.teamSpawnCount() >= 2 ? "2 - " + hoveredArena.teamSpawnCount() + " Teams" : "§cIncomplete")));
            tooltip.add(Component.literal("§7Max Capacity: §f" + (hoveredArena.teamSpawnCount() * hoveredArena.maxPlayersPerTeam()) + " Players"));
            tooltip.add(Component.literal("§7Dimensions: §e" + hoveredArena.sizeX() + "x" + hoveredArena.sizeY() + "x" + hoveredArena.sizeZ() + " blocks"));
            tooltip.add(Component.literal("§7Spectator Spawn: " + (hoveredArena.hasSpectatorSpawn() ? "§a✔ Configured" : "§c✖ Missing")));
            tooltip.add(Component.literal("§7Border Shape: §f" + hoveredArena.borderShape()));

            int tw = 0;
            for (Component c : tooltip) {
                tw = Math.max(tw, Minecraft.getInstance().font.width(c));
            }
            int th = tooltip.size() * 11 + 6;
            int tx = Math.min(mouseX + 10, contentX + contentW - tw - 12);
            int ty = Math.max(contentY, Math.min(mouseY, contentY + contentH - th - 4));

            graphics.fill(tx, ty, tx + tw + 8, ty + th, 0xF8101015);
            graphics.outline(tx, ty, tw + 8, th, 0xFF8888AA);

            int textY = ty + 4;
            for (Component line : tooltip) {
                graphics.text(Minecraft.getInstance().font, line, tx + 4, textY, 0xFFFFFFFF, true);
                textY += 11;
            }
        }
    }
}