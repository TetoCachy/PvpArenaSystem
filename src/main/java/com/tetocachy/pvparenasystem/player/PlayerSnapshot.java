package com.tetocachy.pvparenasystem.player;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

public class PlayerSnapshot {
    private final UUID playerUuid;
    private final ResourceKey<Level> originalDimension;
    private final double x, y, z;
    private final float yaw, pitch;
    private final GameType originalGameMode;
    private final float health;
    private final int foodLevel;
    private final float saturationLevel;
    private final int experienceLevel;
    private final float experienceProgress;
    private final int totalExperience;
    private final ListTag inventoryTag;
    private final ListTag activeEffectsTag;
    private final String reason;

    public PlayerSnapshot(UUID playerUuid, ResourceKey<Level> originalDimension, double x, double y, double z,
                          float yaw, float pitch, GameType originalGameMode, float health, int foodLevel,
                          float saturationLevel, int experienceLevel, float experienceProgress,
                          int totalExperience, ListTag inventoryTag, ListTag activeEffectsTag, String reason) {
        this.playerUuid = playerUuid;
        this.originalDimension = originalDimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.originalGameMode = originalGameMode;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturationLevel = saturationLevel;
        this.experienceLevel = experienceLevel;
        this.experienceProgress = experienceProgress;
        this.totalExperience = totalExperience;
        this.inventoryTag = inventoryTag;
        this.activeEffectsTag = activeEffectsTag;
        this.reason = reason;
    }

    public static PlayerSnapshot capture(ServerPlayer player, String reason) {
        ListTag invTag = new ListTag();
        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                Tag itemTag = ItemStack.CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).result().orElse(null);
                if (itemTag != null) {
                    slotTag.put("Item", itemTag);
                    invTag.add(slotTag);
                }
            }
        }

        ListTag effectsTag = new ListTag();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            Tag effectTag = MobEffectInstance.CODEC.encodeStart(NbtOps.INSTANCE, effect).result().orElse(null);
            if (effectTag != null) {
                effectsTag.add(effectTag);
            }
        }

        return new PlayerSnapshot(
                player.getUUID(),
                player.level().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.gameMode.getGameModeForPlayer(),
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.experienceLevel,
                player.experienceProgress,
                player.totalExperience,
                invTag,
                effectsTag,
                reason
        );
    }

    public void restore(ServerPlayer player) {
        player.getInventory().clearContent();
        player.removeAllEffects();

        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < this.inventoryTag.size(); i++) {
            CompoundTag slotTag = this.inventoryTag.getCompoundOrEmpty(i);
            int slot = slotTag.getIntOr("Slot", -1);
            if (slot >= 0 && slot < containerSize) {
                Tag itemTag = slotTag.get("Item");
                if (itemTag != null) {
                    ItemStack stack = ItemStack.CODEC.parse(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), itemTag).result().orElse(ItemStack.EMPTY);
                    player.getInventory().setItem(slot, stack);
                }
            }
        }
        player.inventoryMenu.broadcastChanges();

        player.setHealth(Math.max(1.0F, this.health));
        player.getFoodData().setFoodLevel(this.foodLevel);
        player.getFoodData().setSaturation(this.saturationLevel);
        player.experienceLevel = this.experienceLevel;
        player.experienceProgress = this.experienceProgress;
        player.totalExperience = this.totalExperience;

        for (int i = 0; i < this.activeEffectsTag.size(); i++) {
            Tag effectTag = this.activeEffectsTag.get(i);
            MobEffectInstance.CODEC.parse(NbtOps.INSTANCE, effectTag).result().ifPresent(player::addEffect);
        }

        player.setGameMode(this.originalGameMode);

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ServerLevel targetLevel = server.getLevel(this.originalDimension);
            if (targetLevel == null) {
                targetLevel = server.overworld();
            }
            player.teleportTo(targetLevel, this.x, this.y, this.z, Set.of(), this.yaw, this.pitch, true);
        }
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("UUID", playerUuid.toString());
        tag.putString("Dimension", originalDimension.identifier().toString());
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putString("GameMode", originalGameMode.getName());
        tag.putFloat("Health", health);
        tag.putInt("FoodLevel", foodLevel);
        tag.putFloat("Saturation", saturationLevel);
        tag.putInt("XpLevel", experienceLevel);
        tag.putFloat("XpProgress", experienceProgress);
        tag.putInt("TotalXp", totalExperience);
        tag.put("Inventory", inventoryTag);
        tag.put("Effects", activeEffectsTag);
        tag.putString("Reason", reason);
        return tag;
    }

    public static PlayerSnapshot fromNbt(CompoundTag tag) {
        UUID uuid = UUID.fromString(tag.getStringOr("UUID", UUID.randomUUID().toString()));
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(tag.getStringOr("Dimension", "minecraft:overworld")));
        double x = tag.getDoubleOr("X", 0.0);
        double y = tag.getDoubleOr("Y", 64.0);
        double z = tag.getDoubleOr("Z", 0.0);
        float yaw = tag.getFloatOr("Yaw", 0.0F);
        float pitch = tag.getFloatOr("Pitch", 0.0F);
        GameType gm = GameType.byName(tag.getStringOr("GameMode", "survival"), GameType.SURVIVAL);
        float health = tag.getFloatOr("Health", 20.0F);
        int food = tag.getIntOr("FoodLevel", 20);
        float sat = tag.getFloatOr("Saturation", 5.0F);
        int xpLvl = tag.getIntOr("XpLevel", 0);
        float xpProg = tag.getFloatOr("XpProgress", 0.0F);
        int totXp = tag.getIntOr("TotalXp", 0);
        ListTag inv = tag.getListOrEmpty("Inventory");
        ListTag eff = tag.getListOrEmpty("Effects");
        String reason = tag.getStringOr("Reason", "UNKNOWN");

        return new PlayerSnapshot(uuid, dim, x, y, z, yaw, pitch, gm, health, food, sat, xpLvl, xpProg, totXp, inv, eff, reason);
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }
}