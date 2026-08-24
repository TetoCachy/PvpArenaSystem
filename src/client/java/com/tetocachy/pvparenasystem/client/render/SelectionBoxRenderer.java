package com.tetocachy.pvparenasystem.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tetocachy.pvparenasystem.client.data.ClientArenaCache;
import com.tetocachy.pvparenasystem.config.ArenaModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SelectionBoxRenderer {

    public static void register() {
        LevelRenderEvents.END_MAIN.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            BlockPos startPos = null;
            BlockPos endPos = null;
            if (ClientArenaCache.hasData() && ClientArenaCache.currentData != null) {
                startPos = ClientArenaCache.currentData.pos1();
                endPos = ClientArenaCache.currentData.pos2();
            }

            boolean isHoldingWand = mc.player.getMainHandItem().is(ArenaModConfig.WAND_ITEM)
                    || mc.player.getOffhandItem().is(ArenaModConfig.WAND_ITEM);

            BlockPos lookingAtPos = null;
            if (mc.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
                lookingAtPos = blockHit.getBlockPos();
            }

            BlockPos resolvedP1;
            BlockPos resolvedP2;
            boolean livePreview;

            if (startPos != null && endPos == null) {
                if (isHoldingWand && lookingAtPos != null) {
                    resolvedP1 = startPos;
                    resolvedP2 = lookingAtPos;
                    livePreview = true;
                } else {
                    resolvedP1 = startPos;
                    resolvedP2 = startPos;
                    livePreview = false;
                }
            } else if (startPos == null && endPos == null) {
                if (isHoldingWand && lookingAtPos != null) {
                    resolvedP1 = lookingAtPos;
                    resolvedP2 = lookingAtPos;
                    livePreview = true;
                } else {
                    return;
                }
            } else {
                resolvedP1 = startPos;
                resolvedP2 = endPos;
                livePreview = false;
            }

            if (resolvedP1 == null || resolvedP2 == null) return;

            final boolean isLivePreview = livePreview;
            final BlockPos anchorPos = startPos;
            final Vec3 camPos = mc.gameRenderer.mainCamera().position();
            PoseStack poseStack = context.poseStack();

            int minBlockX = Math.min(resolvedP1.getX(), resolvedP2.getX());
            int minBlockY = Math.min(resolvedP1.getY(), resolvedP2.getY());
            int minBlockZ = Math.min(resolvedP1.getZ(), resolvedP2.getZ());

            int maxBlockX = Math.max(resolvedP1.getX(), resolvedP2.getX());
            int maxBlockY = Math.max(resolvedP1.getY(), resolvedP2.getY());
            int maxBlockZ = Math.max(resolvedP1.getZ(), resolvedP2.getZ());

            final double minX = minBlockX - camPos.x;
            final double minY = minBlockY - camPos.y;
            final double minZ = minBlockZ - camPos.z;

            final double maxX = maxBlockX + 1.0 - camPos.x;
            final double maxY = maxBlockY + 1.0 - camPos.y;
            final double maxZ = maxBlockZ + 1.0 - camPos.z;

            // Colors:
            // Preview: Warm Amber/Yellow (r=1.0, g=0.85, b=0.2)
            // Confirmed Selection: Bright Neon Cyan (r=0.0, g=1.0, b=1.0)
            final float r = isLivePreview ? 1.0F : 0.0F;
            final float g = isLivePreview ? 0.85F : 1.0F;
            final float b = isLivePreview ? 0.2F : 1.0F;

            // 1. Render See-Through Translucent Volume Fill
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
                renderFilledBox(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 0.20F);
            });

            // 2. Render Wireframe Line Box
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                renderBoxLines(pose, buffer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, isLivePreview ? 0.85F : 1.0F);

                // Highlight Pos 1 Anchor with an orange outline if in live preview mode
                if (isLivePreview && anchorPos != null) {
                    double aMinX = anchorPos.getX() - camPos.x;
                    double aMinY = anchorPos.getY() - camPos.y;
                    double aMinZ = anchorPos.getZ() - camPos.z;
                    renderBoxLines(pose, buffer, aMinX, aMinY, aMinZ, aMinX + 1.0, aMinY + 1.0, aMinZ + 1.0, 1.0F, 0.5F, 0.0F, 1.0F);
                }
            });
        });
    }

    private static void renderFilledBox(PoseStack.Pose pose, VertexConsumer buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        // Bottom (Y-)
        quad(buffer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        // Top (Y+)
        quad(buffer, pose, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        // North (Z-)
        quad(buffer, pose, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        // South (Z+)
        quad(buffer, pose, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
        // West (X-)
        quad(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);
        // East (X+)
        quad(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, float r, float g, float b, float a) {
        buffer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        buffer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        buffer.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        buffer.addVertex(pose, (float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
    }

    private static void renderBoxLines(PoseStack.Pose pose, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        // Bottom square
        line(buffer, pose, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(buffer, pose, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(buffer, pose, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(buffer, pose, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // Top square
        line(buffer, pose, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(buffer, pose, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(buffer, pose, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(buffer, pose, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // Vertical pillars
        line(buffer, pose, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(buffer, pose, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(buffer, pose, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(buffer, pose, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(VertexConsumer b, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2, float red, float green, float blue, float alpha) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = len > 0 ? dx / len : 0.0F;
        float ny = len > 0 ? dy / len : 1.0F;
        float nz = len > 0 ? dz / len : 0.0F;

        b.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(1.0F);

        b.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(1.0F);
    }
}