package com.legoatoom.develop.client.render.entity.model;

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
        this.chainKnot.addCuboid(-3.0F, -6.0F, -3.0F, 6.0F, 8.0F, 6.0F, 0.0F);
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
