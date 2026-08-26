package com.tetocachy.pvparenasystem.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public class ArenaBlockSnapshot {
    private final Map<BlockPos, BlockState> blockMap = new HashMap<>();
    private final BlockPos minPos;
    private final BlockPos maxPos;

    public ArenaBlockSnapshot(BlockPos minPos, BlockPos maxPos) {
        this.minPos = minPos;
        this.maxPos = maxPos;
    }

    public void capture(ServerLevel level) {
        blockMap.clear();
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            blockMap.put(pos.immutable(), level.getBlockState(pos));
        }
    }

    public void pasteToOffset(ServerLevel level, int offsetX, int offsetY, int offsetZ) {
        for (Map.Entry<BlockPos, BlockState> entry : blockMap.entrySet()) {
            BlockPos dest = entry.getKey().offset(offsetX, offsetY, offsetZ);
            level.setBlock(dest, entry.getValue(), 2);
        }
    }

    public void clearToAir(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        clearEntities(level);
    }

    public void rollback(ServerLevel level) {
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState originalState = blockMap.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            BlockState currentState = level.getBlockState(pos);
            if (currentState != originalState) {
                level.setBlock(pos, originalState, 3);
            }
        }
        clearEntities(level);
    }

    private void clearEntities(ServerLevel level) {
        AABB box = new AABB(minPos.getX() - 5, minPos.getY() - 10, minPos.getZ() - 5,
                maxPos.getX() + 6, maxPos.getY() + 30, maxPos.getZ() + 6);
        for (Entity entity : level.getEntities((Entity) null, box, e -> e instanceof ItemEntity || e instanceof Projectile)) {
            entity.discard();
        }
    }
}