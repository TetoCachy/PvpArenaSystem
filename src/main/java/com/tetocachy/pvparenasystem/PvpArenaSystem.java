package com.tetocachy.pvparenasystem;

import com.tetocachy.pvparenasystem.arena.ArenaManager;
import com.tetocachy.pvparenasystem.command.ArenaCommand;
import com.tetocachy.pvparenasystem.command.DuelCommand;
import com.tetocachy.pvparenasystem.command.KitCommand;
import com.tetocachy.pvparenasystem.command.PartyCommand;
import com.tetocachy.pvparenasystem.event.PlayerEventListener;
import com.tetocachy.pvparenasystem.kit.KitManager;
import com.tetocachy.pvparenasystem.match.MatchManager;
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

		// 1. Register Event Listeners
		PlayerEventListener.register();

		// 2. Register Server Tick (Match Engine Loop)
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MatchManager.tickMatches();
		});

		// 3. Register Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ArenaCommand.register(dispatcher);
			KitCommand.register(dispatcher);
			DuelCommand.register(dispatcher);
			PartyCommand.register(dispatcher);
		});

		// 4. Server Lifecycle Hooks
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			KitManager.loadKits(server);
			ArenaManager.loadArenas(server);
			LOGGER.info("PvpArenaSystem successfully loaded kits and arenas!");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			PlayerStateManager.emergencyRestoreAll(server);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}