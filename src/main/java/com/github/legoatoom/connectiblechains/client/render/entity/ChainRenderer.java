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
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ChainRenderer {

    private static final int MAX_CACHE_SIZE = 1000;

    private final Map<BakeKey, ChainModel> models = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BakeKey, ChainModel> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    /**
     * Renders the cached model for the given {@code key}.
     * If a model is not present for the given key it will be built.
     *
     * @param buffer      The target vertex buffer
     * @param matrices    The chain transformation
     * @param chainVec    The vector from the start position to the end position
     * @param blockLight0 The block light level at the start
     * @param blockLight1 The block light level at the end
     * @param skyLight0   The sky light level at the start
     * @param skyLight1   The sky light level at the end
     */
    public void renderBaked(CatenaryRenderer renderer, VertexConsumer buffer, MatrixStack matrices, BakeKey ignoredOldKey, Vector3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        BakeKey key = new BakeKey(chainVec);

        ChainModel model;
        if (models.containsKey(key)) {
            model = models.get(key);
        } else {
            model = renderer.buildModel(chainVec);
            models.put(key, model);
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment() && models.size() > MAX_CACHE_SIZE) {
            ConnectibleChains.LOGGER.warn("Chain model cache full, evicting...");
        }

        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }


    /**
     * Same as {@link #renderBaked but will not use
     * the model cache. This should be used when {@code chainVec} is changed very frequently.
     *
     * @see #renderBaked
     */
    public void render(CatenaryRenderer renderer, VertexConsumer buffer, MatrixStack matrices, Vector3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model = renderer.buildModel(chainVec);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    /**
     * Purge the model cache.
     */
    public void purge() {
        models.clear();
    }

    /**
     * Used to identify a cached model.
     * Chains that have an identical bake key can use the same model as the geometry is the same.
     */
    public static class BakeKey {
        private final Vector3f chainVec;

        public BakeKey(Vector3f chainVec) {
            this.chainVec = new Vector3f(chainVec);
        }

        public BakeKey(Vec3d srcPos, Vec3d dstPos) {
            this.chainVec = new Vector3f((float)(dstPos.x - srcPos.x), (float)(dstPos.y - srcPos.y), (float)(dstPos.z - srcPos.z));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BakeKey bakeKey)) return false;
            return Objects.equals(chainVec, bakeKey.chainVec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chainVec);
        }
    }
}