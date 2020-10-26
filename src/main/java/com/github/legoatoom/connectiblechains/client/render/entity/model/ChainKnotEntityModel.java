/*
 *     Copyright (C) 2020 legoatoom
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity.model;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public class ChainKnotEntityModel<T extends Entity> extends CompositeEntityModel<T> {
    private final ModelPart chainKnot;

    public ChainKnotEntityModel() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.chainKnot = new ModelPart(this, 0, 0);
        this.chainKnot.addCuboid(-3.0F, -6.0F, -3.0F, 6.0F, 3.0F, 6.0F, 0.0F);
        this.chainKnot.setPivot(0.0F, 0.0F, 0.0F);
    }

    public Iterable<ModelPart> getParts() {
        return ImmutableList.of(this.chainKnot);
    }

    public void setAngles(T entity, float limbAngle, float limbDistance, float customAngle, float headYaw, float headPitch) {
        this.chainKnot.yaw = headYaw * 0.017453292F;
        this.chainKnot.pitch = headPitch * 0.017453292F;
    }
}
