/*
 * Copyright (C) 2022 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.LightType;

import java.util.ArrayList;

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
    private static final Identifier KNOT_TEXTURE = Helper.identifier("textures/entity/chain_knot.png");
    private static final Identifier CHAIN_TEXTURE = new Identifier("textures/block/chain.png");
    private final ChainKnotEntityModel<ChainKnotEntity> model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel<>(context.getPart(ClientInitializer.CHAIN_KNOT));
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
        return KNOT_TEXTURE;
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        Vec3d leashOffset = chainKnotEntity.getLeashPos(tickDelta).subtract(chainKnotEntity.getLerpedPos(tickDelta));
        matrices.translate(leashOffset.x, leashOffset.y + 6.5/16f, leashOffset.z);
        matrices.scale(5/6f, 1, 5/6f);
        this.model.setAngles(chainKnotEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(KNOT_TEXTURE));
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
        matrices.pop();
        ArrayList<Entity> entities = chainKnotEntity.getHoldingEntities();
        for (Entity entity : entities) {
            this.createChainLine(chainKnotEntity, tickDelta, matrices, vertexConsumers, entity);
            if(ConnectibleChains.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(matrices, chainKnotEntity, entity, vertexConsumers.getBuffer(RenderLayer.LINES));
            }
        }
        if(ConnectibleChains.runtimeConfig.doDebugDraw()) {
            matrices.push();
            // F stands for "from"
            Text holdingCount = new LiteralText("F: " + chainKnotEntity.getHoldingEntities().size());
            matrices.translate(0, 0.25, 0);
            this.renderLabelIfPresent(chainKnotEntity, holdingCount, matrices, vertexConsumers, light);
            matrices.pop();
        }
        super.render(chainKnotEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    /**
     * Draws a line fromEntity - toEntity, from green to red.
     */
    private void drawDebugVector(MatrixStack matrices, Entity fromEntity, Entity toEntity, VertexConsumer buffer) {
        if(toEntity == null) return;
        Matrix4f modelMat = matrices.peek().getPositionMatrix();
        Vec3d vec = toEntity.getPos().subtract(fromEntity.getPos());
        Vec3d normal = vec.normalize();
        buffer.vertex(modelMat, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal((float)normal.x, (float)normal.y, (float)normal.z).next();
        buffer.vertex(modelMat, (float)vec.x, (float)vec.y, (float)vec.z)
                .color(255, 0, 0, 255)
                .normal((float)normal.x, (float)normal.y, (float)normal.z).next();
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
        if (toEntity == null) return; // toEntity can be null, this will return the function if it is null.
        matrices.push();

        // Don't have to lerp knot position as it can't move
        // Also lerping the position of an entity that was just created
        // causes visual bugs because the position is lerped from 0/0/0.
        Vec3d srcPos = fromEntity.getPos().add(fromEntity.getLeashOffset());
        Vec3d dstPos;

        if(toEntity instanceof AbstractDecorationEntity) {
            dstPos = toEntity.getPos().add(toEntity.getLeashOffset());
        } else {
            dstPos = toEntity.getLeashPos(tickDelta);
        }

        // The leash pos offset
        Vec3d leashOffset = fromEntity.getLeashOffset();
        matrices.translate(leashOffset.x, leashOffset.y, leashOffset.z);

        // Some further performance improvements can be made here:
        // Create a rendering layer that:
        // - does not have normals
        // - does not have an overlay
        // - does not have vertex color
        // - uses a tri strp instead of quads
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutoutNoCull(CHAIN_TEXTURE));
        if(ConnectibleChains.runtimeConfig.doDebugDraw()) {
            buffer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
        }

        Vec3f offset = Helper.getChainOffset(srcPos, dstPos);
        matrices.translate(offset.getX(), 0, offset.getZ());

        // Now we gather light information for the chain. Since the chain is lighter if there is more light.
        BlockPos blockPosOfStart = new BlockPos(fromEntity.getCameraPosVec(tickDelta));
        BlockPos blockPosOfEnd = new BlockPos(toEntity.getCameraPosVec(tickDelta));
        int blockLightLevelOfStart = fromEntity.world.getLightLevel(LightType.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.world.getLightLevel(LightType.BLOCK, blockPosOfEnd);
        int skylightLevelOfStart = fromEntity.world.getLightLevel(LightType.SKY, blockPosOfStart);
        int skylightLevelOfEnd = fromEntity.world.getLightLevel(LightType.SKY, blockPosOfEnd);

        Vec3d startPos = srcPos.add(offset.getX(), 0, offset.getZ());
        Vec3d endPos = dstPos.add(-offset.getX(), 0, -offset.getZ());
        Vec3f chainVec = new Vec3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));

        float angleY = -(float) Math.atan2(chainVec.getZ(), chainVec.getX());
        matrices.multiply(Quaternion.fromEulerXyz(0, angleY, 0));

        if (toEntity instanceof AbstractDecorationEntity) {
            ChainRenderer.BakeKey key = new ChainRenderer.BakeKey(fromEntity.getPos(), toEntity.getPos());
            chainRenderer.renderBaked(buffer, matrices, key, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        } else {
            chainRenderer.render(buffer, matrices, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        }

        matrices.pop();
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }
}
