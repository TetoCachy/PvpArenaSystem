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

    public void rollback(ServerLevel level) {
        // 1. Reset blocks
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState originalState = blockMap.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            BlockState currentState = level.getBlockState(pos);
            if (currentState != originalState) {
                level.setBlock(pos, originalState, 3);
            }
        }

        // 2. Clear dropped items and projectiles inside arena bounds
        AABB box = new AABB(minPos.getX(), minPos.getY(), minPos.getZ(),
                maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1);
        for (Entity entity : level.getEntities(null, box)) {
            if (entity instanceof ItemEntity || entity instanceof Projectile) {
                entity.discard();
            }
        }
    }
}