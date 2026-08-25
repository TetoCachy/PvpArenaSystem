package com.tetocachy.pvparenasystem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.tetocachy.pvparenasystem.PvpArenaSystem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArenaModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Item WAND_ITEM = Items.WOODEN_PICKAXE;
    public static int COUNTDOWN_SECONDS = 5;
    public static int CELEBRATION_SECONDS = 5;
    public static boolean ALLOW_BLOCK_BREAKING = false;
    public static boolean ALLOW_BLOCK_PLACING = false;

    // Modular Admin-Configurable Match Presets
    public static final List<Integer> ALLOWED_TEAM_COUNTS = new ArrayList<>(List.of(2, 3, 4, 8));
    public static final List<Integer> ALLOWED_TEAM_SIZES = new ArrayList<>(List.of(1, 2, 3, 4, 5));
    public static final List<Integer> ALLOWED_GOAL_POINTS = new ArrayList<>(List.of(1, 2, 3, 5, 10));

    private static Path getConfigFile(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("pvparenasystem");
        File dir = path.toFile();
        if (!dir.exists()) dir.mkdirs();
        return path.resolve("config.json");
    }

    public static void loadConfig(MinecraftServer server) {
        Path file = getConfigFile(server);
        if (!file.toFile().exists()) {
            saveConfig(server);
            return;
        }

        try (FileReader reader = new FileReader(file.toFile())) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);
            if (obj.has("teamCounts")) {
                ALLOWED_TEAM_COUNTS.clear();
                for (var el : obj.getAsJsonArray("teamCounts")) ALLOWED_TEAM_COUNTS.add(el.getAsInt());
            }
            if (obj.has("teamSizes")) {
                ALLOWED_TEAM_SIZES.clear();
                for (var el : obj.getAsJsonArray("teamSizes")) ALLOWED_TEAM_SIZES.add(el.getAsInt());
            }
            if (obj.has("goalPoints")) {
                ALLOWED_GOAL_POINTS.clear();
                for (var el : obj.getAsJsonArray("goalPoints")) ALLOWED_GOAL_POINTS.add(el.getAsInt());
            }
            ensureDefaults();
            saveConfig(server);
        } catch (Exception e) {
            PvpArenaSystem.LOGGER.error("Failed to load pvparenasystem config", e);
        }
    }

    public static void saveConfig(MinecraftServer server) {
        try {
            Path file = getConfigFile(server);
            JsonObject obj = new JsonObject();

            JsonArray tc = new JsonArray();
            for (int i : ALLOWED_TEAM_COUNTS) tc.add(i);
            obj.add("teamCounts", tc);

            JsonArray ts = new JsonArray();
            for (int i : ALLOWED_TEAM_SIZES) ts.add(i);
            obj.add("teamSizes", ts);

            JsonArray gp = new JsonArray();
            for (int i : ALLOWED_GOAL_POINTS) gp.add(i);
            obj.add("goalPoints", gp);

            try (FileWriter writer = new FileWriter(file.toFile())) {
                GSON.toJson(obj, writer);
            }
        } catch (Exception e) {
            PvpArenaSystem.LOGGER.error("Failed to save pvparenasystem config", e);
        }
    }

    private static void ensureDefaults() {
        if (ALLOWED_TEAM_COUNTS.isEmpty()) ALLOWED_TEAM_COUNTS.addAll(List.of(2, 3, 4));
        if (ALLOWED_TEAM_SIZES.isEmpty()) ALLOWED_TEAM_SIZES.addAll(List.of(1, 2, 4));
        if (ALLOWED_GOAL_POINTS.isEmpty()) ALLOWED_GOAL_POINTS.addAll(List.of(1, 3, 5));
        Collections.sort(ALLOWED_TEAM_COUNTS);
        Collections.sort(ALLOWED_TEAM_SIZES);
        Collections.sort(ALLOWED_GOAL_POINTS);
    }

    public static void addPreset(MinecraftServer server, String type, int value) {
        if (value <= 0) return;
        List<Integer> target = switch (type) {
            case "TEAM_COUNT" -> ALLOWED_TEAM_COUNTS;
            case "TEAM_SIZE" -> ALLOWED_TEAM_SIZES;
            case "GOAL_POINT" -> ALLOWED_GOAL_POINTS;
            default -> null;
        };

        if (target != null && !target.contains(value)) {
            target.add(value);
            Collections.sort(target);
            saveConfig(server);
        }
    }

    public static void removePreset(MinecraftServer server, String type, int value) {
        List<Integer> target = switch (type) {
            case "TEAM_COUNT" -> ALLOWED_TEAM_COUNTS;
            case "TEAM_SIZE" -> ALLOWED_TEAM_SIZES;
            case "GOAL_POINT" -> ALLOWED_GOAL_POINTS;
            default -> null;
        };

        if (target != null && target.size() > 1) {
            target.remove(Integer.valueOf(value));
            Collections.sort(target);
            saveConfig(server);
        }
    }
}