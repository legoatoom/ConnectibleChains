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

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import java.util.ArrayList;
import java.util.List;

public class ChainModel {
    private final float[] vertices;
    private final float[] uvs;

    public ChainModel(float[] vertices, float[] uvs) {
        this.vertices = vertices;
        this.uvs = uvs;
    }

    public void render(VertexConsumer buffer, MatrixStack matrices, int bLight0, int bLight1, int sLight0, int sLight1) {
        Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        int count = vertices.length / 3;
        for (int i = 0; i < count; i++) {
            // divide by 2 because chain has 2 face sets
            @SuppressWarnings({"IntegerDivisionInFloatingPointContext"})
            float f = (i % (count/2)) / (float) (count/2);
            int blockLight = (int) MathHelper.lerp(f, (float) bLight0, (float) bLight1);
            int skyLight = (int) MathHelper.lerp(f, (float) sLight0, (float) sLight1);
            int light = LightmapTextureManager.pack(blockLight, skyLight);
            buffer
                    .vertex(modelMatrix, vertices[i*3], vertices[i*3+1] , vertices[i*3+2])
                    .color(255, 255, 255, 255)
                    .texture(uvs[i*2], uvs[i*2+1])
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(normalMatrix, 0, 1, 0)
                    .next();
        }
    }

    public static Builder builder(int initialCapacity) {
        return new Builder(initialCapacity);
    }

    public static class Builder {
        private final List<Float> vertices;
        private final List<Float> uvs;
        private int size;

        public Builder(int initialCapacity) {
            vertices = new ArrayList<>(initialCapacity*3);
            uvs = new ArrayList<>(initialCapacity*2);
        }

        public Builder vertex(Vec3f v) {
            vertices.add(v.getX());
            vertices.add(v.getY());
            vertices.add(v.getZ());
            return this;
        }

        public Builder uv(float u, float v) {
            uvs.add(u);
            uvs.add(v);
            return this;
        }

        public void next() {
            size++;
        }

        public ChainModel build() {
            if(vertices.size() != size*3) throw new AssertionError("Wrong count of vertices");
            if(uvs.size() != size*2) throw new AssertionError("Wrong count of uvs");

            return new ChainModel(toFloatArray(vertices), toFloatArray(uvs));
        }

        private float[] toFloatArray(List<Float> floats) {
            float[] array = new float[floats.size()];
            int i = 0;

            for (float f : floats) {
                array[i++] = f;
            }

            return array;
        }
    }
}
