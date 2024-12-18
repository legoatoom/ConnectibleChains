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

import com.github.legoatoom.connectiblechains.client.render.entity.state.ChainKnotEntityRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;

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
public class ChainKnotEntityModel extends EntityModel<ChainKnotEntityRenderState> {

    public ChainKnotEntityModel(ModelPart root) {
        super(root);
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bb_main = modelPartData.addChild("knot", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -12.5F, 0.0F));

        bb_main.addChild("knot_child", ModelPartBuilder.create().uv(3, 1).cuboid(-1.0F, -1.5F, 3.0F, 3.0F, 6.0F, 0.0F, new Dilation(0.0F))
                .uv(0, 1).cuboid(-1.0F, -1.5F, -3.0F, 3.0F, 0.0F, 6.0F, new Dilation(0.0F))
                .uv(0, 9).mirrored().cuboid(-1.0F, 4.5F, -3.0F, 3.0F, 0.0F, 6.0F, new Dilation(0.0F)).mirrored(false)
                .uv(3, 6).cuboid(-1.0F, -1.5F, -3.0F, 3.0F, 6.0F, 0.0F, new Dilation(0.0F)), ModelTransform.of(-1.5F, 7.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        return TexturedModelData.of(modelData, 16, 16);
    }

}
