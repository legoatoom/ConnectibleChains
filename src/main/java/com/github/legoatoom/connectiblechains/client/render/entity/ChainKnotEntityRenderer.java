/*
 * Copyright (C) 2021 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity;

import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.legoatoom.connectiblechains.util.Helper.drip2;

/**
 * <p>This class renders the chain you see in game. The block around the fence and the chain.
 * You could use this code to start to understand how this is done.
 * I tried to make it as easy to understand as possible, mainly for myself, since the MobEntityRenderer has a lot of
 * unclear code and shortcuts made.</p>
 *
 * <p>Following is the formula used. h is the height difference, d is the distance and α is a scaling factor</p>
 *
 * <img src="./doc-files/formula.png">
 *
 * @see net.minecraft.client.render.entity.LeashKnotEntityRenderer
 * @see net.minecraft.client.render.entity.MobEntityRenderer
 * @author legoatoom
 */
@Environment(EnvType.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private static final Identifier TEXTURE = Helper.identifier("textures/entity/chain_knot.png");
    private final ChainKnotEntityModel<ChainKnotEntity> model = new ChainKnotEntityModel<>();


    public ChainKnotEntityRenderer(EntityRenderDispatcher entityRenderDispatcher) {
        super(entityRenderDispatcher);
    }

    /**
     * Draw a pixel with 4 vector locations and the other information.
     */
    private static void renderPixel(Vec3f startA, Vec3f startB, Vec3f endA, Vec3f endB,
                                    VertexConsumer vertexConsumer, Matrix4f matrix4f, int lightPack,
                                    float R, float G, float B) {
        vertexConsumer.vertex(matrix4f, startA.getX(), startA.getY(), startA.getZ()).color(R, G, B, 1.0F).light(lightPack).next();
        vertexConsumer.vertex(matrix4f, startB.getX(), startB.getY(), startB.getZ()).color(R, G, B, 1.0F).light(lightPack).next();

        vertexConsumer.vertex(matrix4f, endB.getX(), endB.getY(), endB.getZ()).color(R, G, B, 1.0F).light(lightPack).next();
        vertexConsumer.vertex(matrix4f, endA.getX(), endA.getY(), endA.getZ()).color(R, G, B, 1.0F).light(lightPack).next();
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum frustum, double x, double y, double z) {
        boolean should = entity.getHoldingEntities().stream().anyMatch(entity1 -> {
            if (entity1 instanceof ChainKnotEntity) {
                if (!entity1.shouldRender(x, y, z)) {
                    return false;
                } else if (entity1.ignoreCameraFrustum) {
                    return true;
                } else {
                    Box box = entity1.getVisibilityBoundingBox().expand(entity.distanceTo(entity1) / 2D);
                    if (box.isValid() || box.getAverageSideLength() == 0.0D) {
                        box = new Box(entity1.getX() - 2.0D, entity1.getY() - 2.0D, entity1.getZ() - 2.0D, entity1.getX() + 2.0D, entity1.getY() + 2.0D, entity1.getZ() + 2.0D);
                    }

                    return frustum.isVisible(box);
                }
            } else return entity1 instanceof PlayerEntity;
        });
        return super.shouldRender(entity, frustum, x, y, z) || should;
    }

    public Identifier getTexture(ChainKnotEntity chainKnotEntity) {
        return TEXTURE;
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.scale(-0.9F, -0.9F, 0.9F);
        this.model.setAngles(chainKnotEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(TEXTURE));
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
        ArrayList<Entity> entities = chainKnotEntity.getHoldingEntities();
        for (Entity entity : entities) {
            if (entity == null || !entity.isAlive() || entity.removed) {
                break;
            }
            this.createChainLine(chainKnotEntity, tickDelta, matrices, vertexConsumers, entity);
        }
        super.render(chainKnotEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    /**
     * If I am honest I do not really know what is happening here most of the time, most of the code was 'inspired' by
     * the {@link net.minecraft.client.render.entity.LeashKnotEntityRenderer}.
     * Many variables therefore have simple names. I tried my best to comment and explain what everything does.
     *
     * @param fromEntity             The origin Entity
     * @param tickDelta              Delta tick
     * @param matrices               The render matrix stack.
     * @param vertexConsumerProvider The VertexConsumerProvider, whatever it does.
     * @param toEntity               The entity that we connect the chain to, this can be a {@link PlayerEntity} or a {@link ChainKnotEntity}.
     */
    private void createChainLine(ChainKnotEntity fromEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, Entity toEntity) {
        matrices.push(); // We push here to start new I think.

        // Some math that has to do with the direction and yaw of the entity to know where to start and end.
        double d = MathHelper.lerp(tickDelta * 0.5F, toEntity.yaw, toEntity.prevYaw) * 0.017453292F;
        double e = MathHelper.lerp(tickDelta * 0.5F, toEntity.pitch, toEntity.prevPitch) * 0.017453292F;
        double g = Math.cos(d);
        double h = Math.sin(d);
        double i = Math.sin(e);

        // Now we have to know whether we connect to a player or a chain to define the start and end points of the chain.
        float lerpDistanceZ, lerpDistanceX, lerpDistanceY;
        if (toEntity instanceof AbstractDecorationEntity) {
            // If the chain is connected to another chain
            double toLerpX = MathHelper.lerp(tickDelta, toEntity.prevX, toEntity.getX());
            double toLerpY = MathHelper.lerp(tickDelta, toEntity.prevY, toEntity.getY());
            double toLerpZ = MathHelper.lerp(tickDelta, toEntity.prevZ, toEntity.getZ());
            double fromLerpX = MathHelper.lerp(tickDelta, fromEntity.prevX, fromEntity.getX());
            double fromLerpY = MathHelper.lerp(tickDelta, fromEntity.prevY, fromEntity.getY());
            double fromLerpZ = MathHelper.lerp(tickDelta, fromEntity.prevZ, fromEntity.getZ());
            lerpDistanceX = (float) (toLerpX - fromLerpX);
            lerpDistanceY = (float) (toLerpY - fromLerpY);
            lerpDistanceZ = (float) (toLerpZ - fromLerpZ);
        } else {
            // If the chain is connected to a player.
            double j = Math.cos(e);
            double k = MathHelper.lerp(tickDelta, toEntity.prevX, toEntity.getX()) - g * 0.7D - h * 0.5D * j;
            double l = MathHelper.lerp(tickDelta, toEntity.prevY
                    + (double) toEntity.getStandingEyeHeight() * 0.7D, toEntity.getY()
                    + (double) toEntity.getStandingEyeHeight() * 0.7D) - i * 0.5D - 0.5D;
            double m = MathHelper.lerp(tickDelta, toEntity.prevZ, toEntity.getZ()) - h * 0.7D + g * 0.5D * j;
            double o = MathHelper.lerp(tickDelta, fromEntity.prevX, fromEntity.getX());
            double p = MathHelper.lerp(tickDelta, fromEntity.prevY, fromEntity.getY()) + 0.3F;
            double q = MathHelper.lerp(tickDelta, fromEntity.prevZ, fromEntity.getZ());

            lerpDistanceX = (float) (k - o);
            lerpDistanceY = (float) (l - p);
            lerpDistanceZ = (float) (m - q);
        }
        matrices.translate(0, 0.3F, 0);

        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLeash());

        //Create offset based on the location. Example that a line that does not travel in the x then the xOffset will be 0.
        float v = MathHelper.fastInverseSqrt(lerpDistanceX * lerpDistanceX + lerpDistanceZ * lerpDistanceZ) * 0.025F / 2;
        float xOffset = lerpDistanceZ * v;
        float zOffset = lerpDistanceX * v;

        // Now we gather light information for the chain. Since the chain is lighter if there is more light.
        BlockPos blockPosOfStart = new BlockPos(fromEntity.getCameraPosVec(tickDelta));
        BlockPos blockPosOfEnd = new BlockPos(toEntity.getCameraPosVec(tickDelta));
        int blockLightLevelOfStart = fromEntity.world.getLightLevel(LightType.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.world.getLightLevel(LightType.BLOCK, blockPosOfEnd);
        int skylightLevelOfStart = fromEntity.world.getLightLevel(LightType.SKY, blockPosOfStart);
        int skylightLevelOfEnd = fromEntity.world.getLightLevel(LightType.SKY, blockPosOfEnd);

        float distance = toEntity.distanceTo(fromEntity);
        Matrix4f matrix4f = matrices.peek().getModel();

        //This number specifies the number of pixels on the chain.
        chainDrawer(distance, vertexConsumer, matrix4f, lerpDistanceX, lerpDistanceY, lerpDistanceZ, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd, xOffset, zOffset);
        matrices.pop();
    }


    /**
     * Fancy math, it deals with the rotation of the x,y,z coordinates based on the direction of the chain. So that
     * in every direction the pixels look the same size.
     *
     */
    private static float[] rotator(double x, double y, double z) {
        double x2 = x * x;
        double z2 = z * z;
        double zx = Math.sqrt(x2 + z2);
        double arc1 = Math.atan2(y, zx);
        double arc2 = Math.atan2(x, z);
        double d = Math.sin(arc1) * 0.0125F;
        float y_new = (float) (Math.cos(arc1) * 0.0125F);
        float z_new = (float) (Math.cos(arc2) * d);
        float x_new = (float) (Math.sin(arc2) * d);
        float v = 0.0F;
        if (zx == 0.0F) {
            x_new = z_new;
            v = 1.0F;
        }
        return new float[]{x_new, y_new, z_new, v};
    }

    /**
     * This method is the big drawer of the chain.
     */
    @SuppressWarnings("DuplicatedCode")
    private void chainDrawer(float distance, VertexConsumer vertexConsumer, Matrix4f matrix4f,
                             float lerpDistanceX, float lerpDistanceY, float lerpDistanceZ,
                             int blockLightLevelOfStart, int blockLightLevelOfEnd,
                             int skylightLevelOfStart, int skylightLevelOfEnd,
                             float xOffset, float zOffset) {

        /*Can you see the chain here?*/
        List<Integer> topLineA, middleLineA, bottomLineA, topLineB, middleLineB, bottomLineB;
        topLineA    = Arrays.asList(   1, 2, 3,       6, 7, 8, 9,         12, 13, 14);
        middleLineA = Arrays.asList(   1,    3,       6,       9,         12,     14);
        bottomLineA = Arrays.asList(   1, 2, 3,       6, 7, 8, 9,         12, 13, 14);

        topLineB    = Arrays.asList(0, 1,    3, 4, 5, 6,       9, 10, 11, 12,     14, 15);
        middleLineB = Arrays.asList(   1,    3,       6,       9,         12,     14    );
        bottomLineB = Arrays.asList(0, 1,    3, 4, 5, 6,       9, 10, 11, 12,     14, 15);

        int length = (int) Math.floor(distance * 24); //This number specifies the number of pixels on the chain.

        // LightLevel Stuff
        float s = (float) skylightLevelOfEnd / (length - 1);
        int t = (int) MathHelper.lerp(s, (float) blockLightLevelOfStart, (float) blockLightLevelOfEnd);
        int u = (int) MathHelper.lerp(s, (float) skylightLevelOfStart, (float) skylightLevelOfEnd);
        int pack = LightmapTextureManager.pack(t, u);

        for (int step = 0; step < length; ++step) {

            float startStepFraction = ((float) step / (float) length);
            float endStepFraction = ((float) (step + 1) / (float) length);
            float startDrip = (float) drip2(startStepFraction * distance, distance, lerpDistanceY);
            float endDrip = (float) drip2(endStepFraction * distance, distance, lerpDistanceY);
            float startRootX = lerpDistanceX * startStepFraction;
            float startRootZ = lerpDistanceZ * startStepFraction;
            float endRootX = lerpDistanceX * endStepFraction;
            float endRootZ = lerpDistanceZ * endStepFraction;
            float[] rotateStartEnd = rotator(startRootX - endRootX, (startDrip - endDrip), startRootZ - endRootZ);
            float v1 = (rotateStartEnd[3] != 1.0F) ? 1.0F : -1.0F;
            float R, G, B;

            float rotate0 = rotateStartEnd[0];
            float rotate1 = rotateStartEnd[1];
            float rotate2 = rotateStartEnd[2];
            // First Line
            float chainHeight = 0.0125F;
            if (topLineA.contains(step % 16)) {
                Vec3f startA, endA, startB, endB;
                startA = new Vec3f(
                        startRootX - rotate0 + xOffset,
                        chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 - zOffset
                );
                startB = new Vec3f(
                        startRootX - (rotate0 - xOffset) * 3,
                        chainHeight + rotate1 * 3 + startDrip,
                        startRootZ - (rotate2 + zOffset) * 3
                );
                endA = new Vec3f(
                        endRootX - rotate0 + xOffset,
                        chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 - zOffset
                );
                endB = new Vec3f(
                        endRootX - (rotate0 - xOffset) * 3,
                        chainHeight + rotate1 * 3 + endDrip,
                        endRootZ - (rotate2 + zOffset) * 3
                );
                R = 0.16F;
                G = 0.17F;
                B = 0.21F;
                renderPixel(startA, startB, endA, endB, vertexConsumer, matrix4f, pack, R, G, B);
            }
            if (middleLineA.contains(step % 16)) {
                Vec3f startA, endA, startB, endB;
                startA = new Vec3f(
                        startRootX + rotate0 + xOffset,
                        chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 - zOffset
                );
                startB = new Vec3f(
                        startRootX - rotate0 - xOffset,
                        chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 + zOffset
                );
                endA = new Vec3f(
                        endRootX + rotate0 + xOffset,
                        chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 - zOffset
                );
                endB = new Vec3f(
                        endRootX - rotate0 - xOffset,
                        chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 + zOffset
                );
                R = 0.12F * 0.7F;
                G = 0.12F * 0.7F;
                B = 0.17F * 0.7F;
                renderPixel(startA, startB, endA, endB, vertexConsumer, matrix4f, pack, R, G, B);
            }
            if (bottomLineA.contains(step % 16)) {
                Vec3f startA, endA, startB, endB;
                startA = new Vec3f(
                        startRootX + (rotate0 - xOffset) * 3,
                        chainHeight - rotate1 * 3 + startDrip,
                        startRootZ + (rotate2 + zOffset) * 3
                );
                startB = new Vec3f(
                        startRootX + rotate0 - xOffset,
                        chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 + zOffset
                );
                endA = new Vec3f(
                        endRootX + (rotate0 - xOffset) * 3,
                        chainHeight - rotate1 * 3 + endDrip,
                        endRootZ + (rotate2 + zOffset) * 3
                );
                endB = new Vec3f(
                        endRootX + rotate0 - xOffset,
                        chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 + zOffset
                );
                R = 0.16F;
                G = 0.17F;
                B = 0.21F;
                renderPixel(startA, startB, endA, endB, vertexConsumer, matrix4f, pack, R, G, B);
            }
            // Second Line
            if (topLineB.contains(step % 16)) {
                Vec3f startA, endA, startB, endB;
                startA = new Vec3f(
                        startRootX - (rotate0 * v1) - xOffset,
                        chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 + zOffset
                );
                startB = new Vec3f(
                        startRootX - ((rotate0 * v1) + xOffset) * 3,
                        chainHeight + rotate1 * 3 + startDrip,
                        startRootZ - (rotate2 - zOffset) * 3
                );
                endA = new Vec3f(
                        endRootX - (rotate0 * v1) - xOffset,
                        chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 + zOffset
                );
                endB = new Vec3f(
                        endRootX - ((rotate0 * v1) + xOffset) * 3,
                        chainHeight + rotate1 * 3 + endDrip,
                        endRootZ - (rotate2 - zOffset) * 3
                );
                R = 0.16F * 0.8F;
                G = 0.17F * 0.8F;
                B = 0.21F * 0.8F;
                renderPixel(startA, startB, endA, endB, vertexConsumer, matrix4f, pack, R, G, B);
            }
            if (middleLineB.contains(step % 16)) {
                Vec3f startA, endA, startB, endB;
                startA = new Vec3f(
                        startRootX + (rotate0 * v1) - xOffset,
                        chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 + zOffset
                );
                startB = new Vec3f(
                        startRootX - (rotate0 * v1) + xOffset,
                        chainHeight + rotate1 + startDrip,
                        startRootZ - rotate2 - zOffset
                );
                endA = new Vec3f(
                        endRootX + (rotate0 * v1) - xOffset,
                        chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 + zOffset
                );
                endB = new Vec3f(
                        endRootX - (rotate0 * v1) + xOffset,
                        chainHeight + rotate1 + endDrip,
                        endRootZ - rotate2 - zOffset
                );
                R = 0.12F;
                G = 0.12F;
                B = 0.17F;
                renderPixel(startA, startB, endA, endB, vertexConsumer, matrix4f, pack, R, G, B);
            }
            if (bottomLineB.contains(step % 16)) {
                Vec3f startA, endA, startB, endB;
                startA = new Vec3f(
                        startRootX + ((rotate0 * v1) + xOffset) * 3,
                        chainHeight - rotate1 * 3 + startDrip,
                        startRootZ + (rotate2 - zOffset) * 3
                );
                startB = new Vec3f(
                        startRootX + (rotate0 * v1) + xOffset,
                        chainHeight - rotate1 + startDrip,
                        startRootZ + rotate2 - zOffset
                );
                endA = new Vec3f(
                        endRootX + ((rotate0 * v1) + xOffset) * 3,
                        chainHeight - rotate1 * 3 + endDrip,
                        endRootZ + (rotate2 - zOffset) * 3
                );
                endB = new Vec3f(
                        endRootX + (rotate0 * v1) + xOffset,
                        chainHeight - rotate1 + endDrip,
                        endRootZ + rotate2 - zOffset
                );
                R = 0.16F * 0.8F;
                G = 0.17F * 0.8F;
                B = 0.21F * 0.8F;
                renderPixel(startA, startB, endA, endB, vertexConsumer, matrix4f, pack, R, G, B);
            }
        }
    }

}
