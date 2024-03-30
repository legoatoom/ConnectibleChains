/*
 * Copyright (C) 2023 legoatoom
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
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
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * <p>This class renders the chain you see in game. The block around the fence and the chain.
 * You could use this code to start to understand how this is done.
 * I tried to make it as easy to understand as possible, mainly for myself, since the MobEntityRenderer has a lot of
 * unclear code and shortcuts made.</p>
 *
 *
 * @author legoatoomm, Qendolin
 * @see net.minecraft.client.render.entity.LeashKnotEntityRenderer
 * @see net.minecraft.client.render.entity.MobEntityRenderer
 */
@Environment(EnvType.CLIENT)
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity> {
    private final ChainKnotEntityModel<ChainKnotEntity> model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel<>(context.getPart(ClientInitializer.CHAIN_KNOT));
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum frustum, double x, double y, double z) {
        if (entity.ignoreCameraFrustum) return true;
        for (ChainLink link : entity.getLinks()) {
            if (link.getPrimary() != entity) continue;
            if (link.getSecondary() instanceof PlayerEntity) return true;
            else if (link.getSecondary().shouldRender(x, y, z)) return true;
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(ChainKnotEntity chainKnotEntity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Render the knot
        if (chainKnotEntity.shouldRenderKnot()) {
            matrices.push();
            Vec3d leashOffset = chainKnotEntity.getLeashPos(tickDelta).subtract(chainKnotEntity.getLerpedPos(tickDelta));
            matrices.translate(leashOffset.x, leashOffset.y + 6.5 / 16f, leashOffset.z);
            // The model is 6 px wide, but it should be rendered at 5px
            matrices.scale(5 / 6f, 1, 5 / 6f);
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(getKnotTexture(chainKnotEntity.getChainItemSource())));
            this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);
            matrices.pop();
        }

        // Render the links
        List<ChainLink> links = chainKnotEntity.getLinks();
        for (ChainLink link : links) {
            if (link.getPrimary() != chainKnotEntity || link.isDead()) continue;
            this.renderChainLink(link, tickDelta, matrices, vertexConsumers);
            if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(matrices, chainKnotEntity, link.getSecondary(), vertexConsumers.getBuffer(RenderLayer.LINES));
            }
        }

        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            matrices.push();
            // F stands for "from", T for "to"

            Text holdingCount = Text.literal("F: " + chainKnotEntity.getLinks().stream()
                    .filter(l -> l.getPrimary() == chainKnotEntity).count());
            Text heldCount = Text.literal("T: " + chainKnotEntity.getLinks().stream()
                    .filter(l -> l.getSecondary() == chainKnotEntity).count());
            matrices.translate(0, 0.25, 0);
            this.renderLabelIfPresent(chainKnotEntity, holdingCount, matrices, vertexConsumers, light);
            matrices.translate(0, 0.25, 0);
            this.renderLabelIfPresent(chainKnotEntity, heldCount, matrices, vertexConsumers, light);
            matrices.pop();
        }
        super.render(chainKnotEntity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private Identifier getKnotTexture(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return new Identifier(id.getNamespace(), "textures/item/" + id.getPath() + ".png");
    }

    private Identifier getChainTexture(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return new Identifier(id.getNamespace(), "textures/block/" + id.getPath() + ".png");
    }

    /**
     * If I am honest I do not really know what is happening here most of the time, most of the code was 'inspired' by
     * the {@link net.minecraft.client.render.entity.LeashKnotEntityRenderer}.
     * Many variables therefore have simple names. I tried my best to comment and explain what everything does.
     *
     * @param link                   A link that provides the positions and type
     * @param tickDelta              Delta tick
     * @param matrices               The render matrix stack.
     * @param vertexConsumerProvider The VertexConsumerProvider, whatever it does.
     */
    private void renderChainLink(ChainLink link, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider) {
        ChainKnotEntity fromEntity = link.getPrimary();
        Entity toEntity = link.getSecondary();
        matrices.push();

        // Don't have to lerp knot position as it can't move
        // Also lerping the position of an entity that was just created
        // causes visual bugs because the position is lerped from 0/0/0.
        Vec3d srcPos = fromEntity.getPos().add(fromEntity.getLeashOffset());
        Vec3d dstPos;

        if (toEntity instanceof AbstractDecorationEntity) {
            dstPos = toEntity.getPos().add(toEntity.getLeashOffset());
        } else {
            dstPos = toEntity.getLeashPos(tickDelta);
        }

        // The leash pos offset
        Vec3d leashOffset = fromEntity.getLeashOffset();
        matrices.translate(leashOffset.x, leashOffset.y, leashOffset.z);

        Item sourceItem = link.getSourceItem();
        // Some further performance improvements can be made here:
        // Create a rendering layer that:
        // - does not have normals
        // - does not have an overlay
        // - does not have vertex color
        // - uses a tri strip instead of quads
        RenderLayer entityCutout = RenderLayer.getEntityCutoutNoCull(getChainTexture(sourceItem));
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(entityCutout);
        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            buffer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
        }

        Vec3d offset = Helper.getChainOffset(srcPos, dstPos);
        matrices.translate(offset.getX(), 0, offset.getZ());

        // Now we gather light information for the chain. Since the chain is lighter if there is more light.
        BlockPos blockPosOfStart = BlockPos.ofFloored(fromEntity.getCameraPosVec(tickDelta));
        BlockPos blockPosOfEnd = BlockPos.ofFloored(toEntity.getCameraPosVec(tickDelta));
        int blockLightLevelOfStart = fromEntity.getWorld().getLightLevel(LightType.BLOCK, blockPosOfStart);
        int blockLightLevelOfEnd = toEntity.getWorld().getLightLevel(LightType.BLOCK, blockPosOfEnd);
        int skylightLevelOfStart = fromEntity.getWorld().getLightLevel(LightType.SKY, blockPosOfStart);
        int skylightLevelOfEnd = fromEntity.getWorld().getLightLevel(LightType.SKY, blockPosOfEnd);

        Vec3d startPos = srcPos.add(offset.getX(), 0, offset.getZ());
        Vec3d endPos = dstPos.add(-offset.getX(), 0, -offset.getZ());
        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));

        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());

        matrices.multiply(new Quaternionf().rotateXYZ(0, angleY, 0));

        if (toEntity instanceof AbstractDecorationEntity) {
            ChainRenderer.BakeKey key = new ChainRenderer.BakeKey(fromEntity.getPos(), toEntity.getPos());
            chainRenderer.renderBaked(buffer, matrices, key, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        } else {
            chainRenderer.render(buffer, matrices, chainVec, blockLightLevelOfStart, blockLightLevelOfEnd, skylightLevelOfStart, skylightLevelOfEnd);
        }

        matrices.pop();
    }



    /**
     * Draws a line fromEntity - toEntity, from green to red.
     */
    private void drawDebugVector(MatrixStack matrices, Entity fromEntity, Entity toEntity, VertexConsumer buffer) {
        if (toEntity == null) return;
        Matrix4f modelMat = matrices.peek().getPositionMatrix();
        Vec3d vec = toEntity.getPos().subtract(fromEntity.getPos());
        Vec3d normal = vec.normalize();
        buffer.vertex(modelMat, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).next();
        buffer.vertex(modelMat, (float) vec.x, (float) vec.y, (float) vec.z)
                .color(255, 0, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).next();
    }

    @Override
    public Identifier getTexture(ChainKnotEntity entity) {
        return getKnotTexture(entity.getChainItemSource());
    }


}
