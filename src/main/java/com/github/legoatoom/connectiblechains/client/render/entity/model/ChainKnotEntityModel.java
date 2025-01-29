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

package com.github.legoatoom.connectiblechains.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;

/**
 * Model for the {@link com.github.legoatoom.connectiblechains.entity.ChainKnotEntity}.
 * Similar to the {@link net.minecraft.client.render.entity.model.LeashKnotEntityModel} code.
 * <p>
 * The model is 6x3x6 pixels big.
 *
 * @author legoatoom
 * @see net.minecraft.client.render.entity.LeashKnotEntityRenderer
 */
@Environment(EnvType.CLIENT)
public class ChainKnotEntityModel<T extends Entity> extends SinglePartEntityModel<T> {
    /**
     * The key of the knot model part, whose value is {@value}.
     */
    private static final String KNOT = "knot";
    private final ModelPart root;
    private final ModelPart knot;

    public ChainKnotEntityModel(ModelPart root) {
        this.root = root;
        this.knot = root.getChild(KNOT);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.knot.yaw = headYaw * (float) (Math.PI / 180.0);
        this.knot.pitch = headPitch * (float) (Math.PI / 180.0);
    }

    /**
     * Exported from Blockbench
     */
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData knot = modelPartData.addChild("knot", ModelPartBuilder.create(), ModelTransform.of(0.0F, -7F, 0.0F, 0.0F, 0.0F, -3.1416F));

        ModelPartData south_r1 = knot.addChild("south_r1", ModelPartBuilder.create().uv(-4, 3).mirrored().cuboid(-2.5F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(0.0F, -0.5F, 3.0F, 1.5708F, 0.0F, -1.5708F));

        ModelPartData north_r1 = knot.addChild("north_r1", ModelPartBuilder.create().uv(-4, 3).cuboid(-2.0F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -0.0F, -3.0F, 1.5708F, 0.0F, 1.5708F));

        ModelPartData east_r1 = knot.addChild("east_r1", ModelPartBuilder.create().uv(-4, 8).cuboid(-2.0F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new Dilation(0.0F)), ModelTransform.of(-3.0F, -0.0F, 0.0F, -3.1416F, 0.0F, 1.5708F));

        ModelPartData west_r1 = knot.addChild("west_r1", ModelPartBuilder.create().uv(-4, 8).mirrored().cuboid(-2.5F, 0.0F, -3.0F, 4.0F, 0.0F, 6.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(3.0F, -0.5F, 0.0F, 0.0F, 0.0F, -1.5708F));
        return TexturedModelData.of(modelData, 16, 16);
    }
}
