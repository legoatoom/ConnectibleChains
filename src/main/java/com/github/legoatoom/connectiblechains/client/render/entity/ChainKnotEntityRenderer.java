/*
 * Copyright (C) 2024 legoatoom.
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
import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.ICatenaryRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import com.github.legoatoom.connectiblechains.client.render.entity.texture.ChainTextureManager;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;

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
public class ChainKnotEntityRenderer extends EntityRenderer<ChainKnotEntity, ChainKnotEntityRenderState> {
    private final ChainKnotEntityModel model;
    private final ChainRenderer chainRenderer = new ChainRenderer();

    public ChainKnotEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.model = new ChainKnotEntityModel(context.getPart(ClientInitializer.CHAIN_KNOT));
    }

    public ChainRenderer getChainRenderer() {
        return chainRenderer;
    }

    @Override
    public boolean shouldRender(ChainKnotEntity entity, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(entity, frustum, x, y, z)) {
            return true;
        }
        for (Chainable.ChainData chainData : new HashSet<>(entity.getChainDataSet())) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder != null) {
                if (frustum.isVisible(chainHolder.getBoundingBox())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ChainKnotEntityRenderState createRenderState() {
        return new ChainKnotEntityRenderState();
    }

    @Override
    public void render(ChainKnotEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Render the knot
        matrices.push();
        matrices.translate(0, 0.7, 0);
        matrices.scale(5 / 6f, 1, 5 / 6f);
        this.model.setAngles(state);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(getKnotTexture(state.sourceItem)));
        this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();

        HashSet<ChainKnotEntityRenderState.ChainData> chainDataSet = state.chainDataSet;
        for (ChainKnotEntityRenderState.ChainData chainData : chainDataSet) {
            renderChainLink(matrices, vertexConsumers, chainData);
            if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
                this.drawDebugVector(matrices, chainData.startPos, chainData.endPos, vertexConsumers.getBuffer(RenderLayer.LINES));
            }
        }

        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            matrices.push();
            Text holdingCount = Text.literal("C: " + chainDataSet.size());
            this.renderLabelIfPresent(state, holdingCount, matrices, vertexConsumers, light);
            matrices.pop();
        }
        super.render(state, matrices, vertexConsumers, light);
    }


    private void renderChainLink(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, ChainKnotEntityRenderState.ChainData chainData) {
        Vec3d offset = chainData.offset;
        Vec3d startPos = chainData.startPos;
        Vec3d endPos = chainData.endPos;
        Item sourceItem = chainData.sourceItem;
        int chainedEntityBlockLight = chainData.chainedEntityBlockLight;
        int chainHolderBlockLight = chainData.chainHolderBlockLight;
        int chainedEntitySkyLight = chainData.chainedEntitySkyLight;
        int chainHolderSkyLight = chainData.chainHolderSkyLight;

        RenderLayer entityCutout = RenderLayer.getEntityCutoutNoCull(getChainTexture(sourceItem));
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(entityCutout);
        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getLines());
        }

        matrices.push();

        // The leash pos offset
        matrices.translate(offset);


        // TODO: Document what this does.
        Vector3f chainVec = new Vector3f((float) (endPos.x - startPos.x), (float) (endPos.y - startPos.y), (float) (endPos.z - startPos.z));
        float angleY = -(float) Math.atan2(chainVec.z(), chainVec.x());
        matrices.multiply(new Quaternionf().rotateXYZ(0, angleY, 0));

        ICatenaryRenderer renderer = getCatenaryRenderer(sourceItem);

        if (chainData.useBaked) {
            ChainRenderer.BakeKey key = new ChainRenderer.BakeKey(startPos, endPos);
            chainRenderer.renderBaked(renderer, vertexConsumer, matrices, key, chainVec, chainedEntityBlockLight, chainHolderBlockLight, chainedEntitySkyLight, chainHolderSkyLight);
        } else {
            chainRenderer.render(renderer, vertexConsumer, matrices, chainVec, chainedEntityBlockLight, chainHolderBlockLight, chainedEntitySkyLight, chainHolderSkyLight);
        }

        matrices.pop();
    }

    /**
     * Draws a line fromEntity - toEntity, from green to red.
     */
    private void drawDebugVector(MatrixStack matrices, Vec3d startPos, Vec3d endPos, VertexConsumer buffer) {
        if (startPos == null) return;
        Matrix4f modelMat = matrices.peek().getPositionMatrix();
        Vec3d vec = endPos.subtract(startPos);
        Vec3d normal = vec.normalize();
        buffer.vertex(modelMat, 0, 0, 0)
                .color(0, 255, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z);
        buffer.vertex(modelMat, (float) vec.x, (float) vec.y, (float) vec.z)
                .color(255, 0, 0, 255)
                .normal((float) normal.x, (float) normal.y, (float) normal.z);
    }


    @Override
    public void updateRenderState(ChainKnotEntity entity, ChainKnotEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        HashSet<ChainKnotEntityRenderState.ChainData> result = new HashSet<>(entity.getChainDataSet().size());
        for (Chainable.ChainData chainData : new HashSet<>(entity.getChainDataSet())) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder == null) {
                continue;
            }

            Vec3d offset = new Vec3d(0, 0.3, 0);
            Vec3d srcPos = entity.getChainPos(tickDelta);
            Vec3d dstPos;
            if (chainHolder instanceof ChainKnotEntity chainKnotEntity) {
                dstPos = chainKnotEntity.getChainPos(tickDelta);
            } else {
                dstPos = chainHolder.getLeashPos(tickDelta);
            }

            BlockPos blockPosOfStart = BlockPos.ofFloored(entity.getCameraPosVec(tickDelta));
            BlockPos blockPosOfEnd = BlockPos.ofFloored(chainHolder.getCameraPosVec(tickDelta));
            World world = entity.getWorld();


            ChainKnotEntityRenderState.ChainData renderChainData = new ChainKnotEntityRenderState.ChainData();
            renderChainData.offset = offset;
            renderChainData.startPos = srcPos;
            renderChainData.endPos = dstPos;
            renderChainData.chainedEntityBlockLight = world.getLightLevel(LightType.BLOCK, blockPosOfStart);
            renderChainData.chainHolderBlockLight = world.getLightLevel(LightType.BLOCK, blockPosOfEnd);
            renderChainData.chainedEntitySkyLight = world.getLightLevel(LightType.SKY, blockPosOfStart);
            renderChainData.chainHolderSkyLight = world.getLightLevel(LightType.SKY, blockPosOfEnd);
            renderChainData.sourceItem = chainData.sourceItem;
            renderChainData.useBaked = chainHolder instanceof BlockAttachedEntity;
            result.add(renderChainData);
        }
        state.chainDataSet = result;
        state.sourceItem = entity.getSourceItem();
        if (ConnectibleChains.runtimeConfig.doDebugDraw()) {
            state.nameLabelPos = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getLerpedYaw(tickDelta));
        }
    }

    private ChainTextureManager getTextureManager() {
        return ClientInitializer.getInstance().getChainTextureManager();
    }

    private Identifier getKnotTexture(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getTextureManager().getKnotTexture(id);
    }

    private Identifier getChainTexture(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getTextureManager().getChainTexture(id);
    }

    private ICatenaryRenderer getCatenaryRenderer(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getTextureManager().getCatenaryRenderer(id);
    }
}
