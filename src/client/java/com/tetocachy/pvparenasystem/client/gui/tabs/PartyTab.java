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

import java.util.function.Consumer;

public class PartyTab implements ArenaScreenTab {
    private int contentX, contentY, contentW, contentH;

    @Override
    public Component getTitle() { return Component.literal("Party Management"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.SHIELD); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = width;
        this.contentH = height;

        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload.PartyInfo party = ClientArenaCache.currentData.party();

        if (!party.inParty()) {
            addWidget.accept(Button.builder(Component.literal("§a+ Create New Party"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("PARTY_CREATE", "", "", 0, 0));
            }).bounds(x + 4, y + 4, width - 8, 20).build());

            int pubY = y + 42;
            for (S2CSyncArenaDataPayload.PublicPartyInfo pub : ClientArenaCache.currentData.publicParties()) {
                addWidget.accept(Button.builder(Component.literal("§e" + pub.partyName() + " §7(Leader: " + pub.leaderName() + ") §b[" + pub.memberCount() + "/" + pub.maxMembers() + "] §a[JOIN]"), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("PARTY_JOIN_PUBLIC", pub.partyName(), "", 0, 0));
                }).bounds(x + 4, pubY, width - 8, 18).build());
                pubY += 20;
            }
        } else {
            if (party.isLeader()) {
                EditBox renameBox = new EditBox(Minecraft.getInstance().font, x + 4, y + 4, 150, 18, Component.literal("Party Name"));
                renameBox.setValue(party.partyName());
                addWidget.accept(renameBox);

                addWidget.accept(Button.builder(Component.literal("Rename"), b -> {
                    if (!renameBox.getValue().isBlank()) {
                        ClientPlayNetworking.send(new C2SActionPayload("PARTY_RENAME", renameBox.getValue(), "", 0, 0));
                    }
                }).bounds(x + 158, y + 4, 55, 18).build());

                addWidget.accept(Button.builder(Component.literal("Max: " + party.maxMembers()), b -> {
                    int next = party.maxMembers() >= 16 ? 2 : party.maxMembers() + 2;
                    ClientPlayNetworking.send(new C2SActionPayload("PARTY_SET_MAX_MEMBERS", "", "", next, 0));
                }).bounds(x + 218, y + 4, 55, 18).build());

                String visibility = party.isPublic() ? "§aPUBLIC" : "§cPRIVATE";
                addWidget.accept(Button.builder(Component.literal(visibility), b -> {
                    ClientPlayNetworking.send(new C2SActionPayload("PARTY_TOGGLE_PUBLIC", "", "", 0, 0));
                }).bounds(x + 278, y + 4, width - 282, 18).build());
            }

            addWidget.accept(Button.builder(Component.literal("§cLeave / Disband Party"), b -> {
                ClientPlayNetworking.send(new C2SActionPayload("PARTY_LEAVE", "", "", 0, 0));
            }).bounds(x + 4, y + height - 22, width - 8, 20).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload.PartyInfo party = ClientArenaCache.currentData.party();

        if (party.inParty()) {
            int top = contentY + 28;
            graphics.fill(contentX + 4, top, contentX + contentW - 4, top + contentH - 54, 0xF0181A22);
            graphics.outline(contentX + 4, top, contentW - 8, contentH - 54, 0xFF4A5060);

            graphics.text(Minecraft.getInstance().font, Component.literal("§6Party: §e" + party.partyName() + " §7(" + party.members().size() + "/" + party.maxMembers() + ")"), contentX + 8, top + 6, 0xFFFFFFFF, false);
            graphics.text(Minecraft.getInstance().font, Component.literal("§7Leader: §e★ " + party.leaderName()), contentX + 8, top + 18, 0xFFFFFF88, false);

            int my = top + 34;
            graphics.text(Minecraft.getInstance().font, Component.literal("§fMembers:"), contentX + 8, my, 0xFFCCCCCC, false);
            my += 12;

            for (String m : party.members()) {
                boolean isL = m.equalsIgnoreCase(party.leaderName());
                graphics.text(Minecraft.getInstance().font, Component.literal((isL ? "§e★ §f" : "§a• §f") + m), contentX + 14, my, 0xFFFFFFFF, false);
                my += 11;
                if (my > top + contentH - 65) break;
            }
        } else {
            graphics.text(Minecraft.getInstance().font, Component.literal("§6Public Parties:"), contentX + 6, contentY + 28, 0xFFFFFFFF, false);
            if (ClientArenaCache.currentData.publicParties().isEmpty()) {
                graphics.text(Minecraft.getInstance().font, Component.literal("§7No public parties active."), contentX + 10, contentY + 44, 0xFFAAAAAA, false);
            }
        }
    }
}