package com.tetocachy.pvparenasystem.client.gui.tabs;

import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
import com.tetocachy.pvparenasystem.client.gui.ArenaScreenTab;
import com.tetocachy.pvparenasystem.network.C2SActionPayload;
import com.tetocachy.pvparenasystem.network.S2CSyncArenaDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public class DuelsTab implements ArenaScreenTab {
    private int selectedPlayerIndex = 0;
    private int selectedKitIndex = 0;
    private int rounds = 1;

    @Override
    public Component getTitle() { return Component.literal("Duels & Matchmaking"); }
    @Override
    public ItemStack getIcon() { return new ItemStack(Items.DIAMOND_SWORD); }

    @Override
    public void init(int x, int y, int width, int height, Consumer<AbstractWidget> addWidget) {
        if (!ClientArenaCache.hasData()) return;
        S2CSyncArenaDataPayload data = ClientArenaCache.currentData;

        List<String> players = data.onlinePlayers();
        String currentTarget = players.isEmpty() ? "No players online" : players.get(Math.min(selectedPlayerIndex, players.size() - 1));

        // 1. Opponent Selector
        Button playerBtn = Button.builder(Component.literal("Opponent: §e" + currentTarget), b -> {
            if (!players.isEmpty()) {
                selectedPlayerIndex = (selectedPlayerIndex + 1) % players.size();
                b.setMessage(Component.literal("Opponent: §e" + players.get(selectedPlayerIndex)));
            }
        }).bounds(x + 5, y + 6, width - 10, 20).build();
        addWidget.accept(playerBtn);

        // 2. Kit Selector
        List<S2CSyncArenaDataPayload.KitInfo> kits = data.kits();
        String currentKit = kits.isEmpty() ? "Default Gear" : kits.get(Math.min(selectedKitIndex, kits.size() - 1)).displayName();
        Button kitBtn = Button.builder(Component.literal("Kit: §b" + currentKit), b -> {
            if (!kits.isEmpty()) {
                selectedKitIndex = (selectedKitIndex + 1) % kits.size();
                b.setMessage(Component.literal("Kit: §b" + kits.get(selectedKitIndex).displayName()));
            }
        }).bounds(x + 5, y + 30, width - 10, 20).build();
        addWidget.accept(kitBtn);

        // 3. Rounds Selector
        Button roundsBtn = Button.builder(Component.literal("Format: §aBest of " + rounds + " (" + rounds + " " + (rounds == 1 ? "round" : "rounds") + ")"), b -> {
            rounds = rounds >= 5 ? 1 : rounds + 2;
            b.setMessage(Component.literal("Format: §aBest of " + rounds + " (" + rounds + " " + (rounds == 1 ? "round" : "rounds") + ")"));
        }).bounds(x + 5, y + 54, width - 10, 20).build();
        addWidget.accept(roundsBtn);

        // 4. Send Challenge Button
        Button challengeBtn = Button.builder(Component.literal("§a§l⚔ SEND DUEL CHALLENGE ⚔"), b -> {
            if (!players.isEmpty()) {
                String target = players.get(selectedPlayerIndex);
                String kitId = kits.isEmpty() ? "" : kits.get(selectedKitIndex).id();
                ClientPlayNetworking.send(new C2SActionPayload("DUEL_SEND", target, kitId, rounds, 0));
            }
        }).bounds(x + 5, y + 84, width - 10, 22).build();
        addWidget.accept(challengeBtn);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}
}