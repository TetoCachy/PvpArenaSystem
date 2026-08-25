package com.tetocachy.pvparenasystem;

import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.command.*;
import com.tetocachy.pvparenasystem.config.ArenaModConfig;
import com.tetocachy.pvparenasystem.event.PlayerEventListener;
import com.tetocachy.pvparenasystem.kit.KitManager;
import com.tetocachy.pvparenasystem.match.MatchManager;
import com.tetocachy.pvparenasystem.network.ModPackets;
import com.tetocachy.pvparenasystem.player.PlayerStateManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PvpArenaSystem implements ModInitializer {
	public static final String MOD_ID = "pvparenasystem";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing PvpArenaSystem...");

		PlayerEventListener.register();

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MatchManager.tickMatches();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ArenaCommand.register(dispatcher);
			KitCommand.register(dispatcher);
			PartyCommand.register(dispatcher);
			MenuCommand.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ArenaModConfig.loadConfig(server);
			KitManager.loadKits(server);
			ArenaManager.loadArenas(server);
			LOGGER.info("PvpArenaSystem successfully loaded configuration, kits, and arenas!");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			PlayerStateManager.emergencyRestoreAll(server);
		});

		ModPackets.registerCommon();
		ModPackets.registerServerReceivers();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}