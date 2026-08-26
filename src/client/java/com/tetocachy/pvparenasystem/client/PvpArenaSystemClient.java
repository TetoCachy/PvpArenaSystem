package com.tetocachy.pvparenasystem.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.tetocachy.pvparenasystem.PvpArenaSystem;
import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
import com.tetocachy.pvparenasystem.client.gui.ArenaMainMenuScreen;
import com.tetocachy.pvparenasystem.client.render.SelectionBoxRenderer;
import com.tetocachy.pvparenasystem.network.S2CSyncArenaDataPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class PvpArenaSystemClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(PvpArenaSystem.MOD_ID, "general")
	);

	public static KeyMapping OPEN_MENU_KEY;

	@Override
	public void onInitializeClient() {
		SelectionBoxRenderer.register();

		OPEN_MENU_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.pvparenasystem.open_menu",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				CATEGORY
		));

		ClientPlayNetworking.registerGlobalReceiver(S2CSyncArenaDataPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				ClientArenaCache.update(payload);

				// Auto-close menu if player was thrown into an active fight
				if (payload.inMatch() && Minecraft.getInstance().gui.screen() instanceof ArenaMainMenuScreen) {
					Minecraft.getInstance().gui.setScreen(null);
				} else if (Minecraft.getInstance().gui.screen() instanceof ArenaMainMenuScreen menu) {
					menu.rebuildTabContent();
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_MENU_KEY.consumeClick()) {
				if (client.player != null) {
					if (ClientArenaCache.hasData() && ClientArenaCache.currentData.inMatch()) {
						client.player.sendSystemMessage(Component.literal("§c§l[!] You cannot open the PvP menu while fighting in a match!"));
					} else {
						client.gui.setScreen(new ArenaMainMenuScreen());
					}
				}
			}
		});
	}
}