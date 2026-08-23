package com.tetocachy.pvparenasystem.kit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class Kit {
    private final String id;
    private final String displayName;
    private final ListTag inventoryTag;

    public Kit(String id, String displayName, ListTag inventoryTag) {
        this.id = id;
        this.displayName = displayName;
        this.inventoryTag = inventoryTag;
    }

    public static Kit fromPlayer(String id, String displayName, ServerPlayer player) {
        ListTag tag = new ListTag();
        int containerSize = player.getInventory().getContainerSize();
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                Tag itemTag = ItemStack.CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).result().orElse(null);
                if (itemTag != null) {
                    slotTag.put("Item", itemTag);
                    tag.add(slotTag);
                }
            }
        }
        return new Kit(id, displayName, tag);
    }

    public void apply(ServerPlayer player) {
        player.getInventory().clearContent();
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
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.put("Inventory", inventoryTag);
        return tag;
    }

    public static Kit fromNbt(CompoundTag tag) {
        return new Kit(
                tag.getStringOr("Id", ""),
                tag.getStringOr("DisplayName", ""),
                tag.getListOrEmpty("Inventory")
        );
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
}