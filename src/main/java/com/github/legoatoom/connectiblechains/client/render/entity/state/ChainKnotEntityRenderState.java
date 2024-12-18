package com.github.legoatoom.connectiblechains.client.render.entity.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;

public class ChainKnotEntityRenderState extends EntityRenderState {
    public HashSet<ChainData> chainDataSet = new HashSet<>();
    public Item sourceItem;


    @Environment(EnvType.CLIENT)
    public static class ChainData {
        public boolean useBaked;
        public Item sourceItem;
        public Vec3d offset = Vec3d.ZERO;
        public Vec3d startPos = Vec3d.ZERO;
        public Vec3d endPos = Vec3d.ZERO;
        public int chainedEntityBlockLight = 0;
        public int chainHolderBlockLight = 0;
        public int chainedEntitySkyLight = 15;
        public int chainHolderSkyLight = 15;
    }
}
