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
import com.github.legoatoom.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

import static com.github.legoatoom.connectiblechains.util.Helper.drip2;
import static com.github.legoatoom.connectiblechains.util.Helper.drip2prime;

public class ChainRenderer {
    /**
     * Changes the width of the chain but does not cause uv distortion.
     */
    private static final float CHAIN_SCALE = 1f;
    /**
     * How many mesh segments a chain is allowed to have.
     * This is to prevent extreme lag and the possibility of an infinite loop.
     */
    private static final int MAX_SEGMENTS = 2048;
    /**
     * The geometry of a chain only depends on the vector from the source to the destination.
     * The rotation/direction and translation of the chain do not matter as they are accounted for during rendering.
     */
    @Deprecated
    private final Object2ObjectOpenHashMap<BakeKey, ChainModel> models = new Object2ObjectOpenHashMap<>(256);

    /**
     * Renders the cached model for the given {@code key}.
     * If a model is not present for the given key it will be built.
     *
     * @param buffer      The target vertex buffer
     * @param matrices    The chain transformation
     * @param key         The cache key for the {@code chainVec}
     * @param chainVec    The vector from the start position to the end position
     * @param blockLight0 The block light level at the start
     * @param blockLight1 The block light level at the end
     * @param skyLight0   The sky light level at the start
     * @param skyLight1   The sky light level at the end
     */
    public void renderBaked(VertexConsumer buffer, MatrixStack matrices, BakeKey key, Vec3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model;
        if (models.containsKey(key)) {
            model = models.get(key);
        } else {
            model = buildModel(chainVec);
            models.put(key, model);
            if (FabricLoader.getInstance().isDevelopmentEnvironment() && models.size() > 10000) {
                ConnectibleChains.LOGGER.error("Chain model leak found!");
            }
        }
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    /**
     * Generates a new baked chain model for the given vector.
     *
     * @param chainVec The vector from the chain start to the end
     * @return The generated model
     */
    private ChainModel buildModel(Vec3f chainVec) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        int initialCapacity = (int) (2f * Helper.lengthOf(chainVec) / desiredSegmentLength);
        ChainModel.Builder builder = ChainModel.builder(initialCapacity);

        if (chainVec.getX() == 0 && chainVec.getZ() == 0) {
            buildFaceVertical(builder, chainVec, 45, UVRect.DEFAULT_SIDE_A);
            buildFaceVertical(builder, chainVec, -45, UVRect.DEFAULT_SIDE_B);
        } else {
            buildFace(builder, chainVec, 45, UVRect.DEFAULT_SIDE_A);
            buildFace(builder, chainVec, -45, UVRect.DEFAULT_SIDE_B);
        }

        return builder.build();
    }

    /**
     * {@link #buildFace} does not work when {@code v} is pointing straight up or down.
     */
    private void buildFaceVertical(ChainModel.Builder builder, Vec3f v, float angle, UVRect uv) {
        float actualSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        float chainWidth = (uv.x1() - uv.x0()) / 16 * CHAIN_SCALE;

        Vec3f normal = new Vec3f((float) Math.cos(Math.toRadians(angle)), 0, (float) Math.sin(Math.toRadians(angle)));
        normal.scale(chainWidth);

        Vec3f vert00 = new Vec3f(-normal.getX() / 2, 0, -normal.getZ() / 2), vert01 = vert00.copy();
        vert01.add(normal);
        Vec3f vert10 = new Vec3f(-normal.getX() / 2, 0, -normal.getZ() / 2), vert11 = vert10.copy();
        vert11.add(normal);

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            if (vert00.getY() + actualSegmentLength >= v.getY()) {
                lastIter = true;
                actualSegmentLength = v.getY() - vert00.getY();
            }

            vert10.add(0, actualSegmentLength, 0);
            vert11.add(0, actualSegmentLength, 0);

            uvv1 += actualSegmentLength / CHAIN_SCALE;

            builder.vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
            builder.vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
            builder.vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
            builder.vertex(vert10).uv(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            uvv0 = uvv1;

            vert00.set(vert10);
            vert01.set(vert11);
        }
    }

    /**
     * Creates geometry from the origin to {@code v} with the specified {@code angle}.
     * It uses an iterative approach meaning that it adds geometry until it's at the end or
     * has reached {@link #MAX_SEGMENTS}.
     * The model is always generated along the local X axis and curves along the Y axis.
     * This makes the calculation a lot simpler as we are only dealing with 2d coordinates.
     *
     * @param builder The target builder
     * @param v       The end position in relation to the origin
     * @param angle   The angle of the face
     * @param uv      The uv bounds of the face
     */
    private void buildFace(ChainModel.Builder builder, Vec3f v, float angle, UVRect uv) {
        float actualSegmentLength, desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        float distance = Helper.lengthOf(v), distanceXZ = (float) Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
        // Original code used total distance between start and end instead of horizontal distance
        // That changed the look of chains when there was a big height difference, but it looks better.
        float wrongDistanceFactor = distance / distanceXZ;

        // 00, 01, 11, 11 refers to the X and Y position of the vertex.
        // 00 is the lower X and Y vertex. 10 Has the same y value as 00 but a higher x value.
        Vec3f vert00 = new Vec3f(), vert01 = new Vec3f(), vert11 = new Vec3f(), vert10 = new Vec3f();
        Vec3f normal = new Vec3f(), rotAxis = new Vec3f();

        float chainWidth = (uv.x1() - uv.x0()) / 16 * CHAIN_SCALE;
        //
        float uvv0, uvv1 = 0, gradient, x, y;
        Vec3f point0 = new Vec3f(), point1 = new Vec3f();
        Quaternion rotator;

        // All of this setup can probably go, but I can't figure out
        // how to integrate it into the loop :shrug:
        point0.set(0, (float) drip2(0, distance, v.getY()), 0);
        gradient = (float) drip2prime(0, distance, v.getY());
        normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
        normal.normalize();

        x = estimateDeltaX(desiredSegmentLength, gradient);
        gradient = (float) drip2prime(x * wrongDistanceFactor, distance, v.getY());
        y = (float) drip2(x * wrongDistanceFactor, distance, v.getY());
        point1.set(x, y, 0);

        rotAxis.set(point1.getX() - point0.getX(), point1.getY() - point0.getY(), point1.getZ() - point0.getZ());
        rotAxis.normalize();
        rotator = rotAxis.getDegreesQuaternion(angle);

        normal.rotate(rotator);
        normal.scale(chainWidth);
        vert10.set(point0.getX() - normal.getX() / 2, point0.getY() - normal.getY() / 2, point0.getZ() - normal.getZ() / 2);
        vert11.set(vert10);
        vert11.add(normal);

        actualSegmentLength = Helper.distanceBetween(point0, point1);

        // This is a pretty simple algorithm to convert the mathematical curve to a model.
        // It uses an incremental approach, adding segments until the end is reached.
        boolean lastIter = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            rotAxis.set(point1.getX() - point0.getX(), point1.getY() - point0.getY(), point1.getZ() - point0.getZ());
            rotAxis.normalize();
            rotator = rotAxis.getDegreesQuaternion(angle);

            // This normal is orthogonal to the face normal
            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotate(rotator);
            normal.scale(chainWidth);

            vert00.set(vert10);
            vert01.set(vert11);

            vert10.set(point1.getX() - normal.getX() / 2, point1.getY() - normal.getY() / 2, point1.getZ() - normal.getZ() / 2);
            vert11.set(vert10);
            vert11.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            builder.vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
            builder.vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
            builder.vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
            builder.vertex(vert10).uv(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            point0.set(point1);

            x += estimateDeltaX(desiredSegmentLength, gradient);
            if (x >= distanceXZ) {
                lastIter = true;
                x = distanceXZ;
            }

            gradient = (float) drip2prime(x * wrongDistanceFactor, distance, v.getY());
            y = (float) drip2(x * wrongDistanceFactor, distance, v.getY());
            point1.set(x, y, 0);

            actualSegmentLength = Helper.distanceBetween(point0, point1);
        }
    }

    /**
     * Estimate Δx based on current gradient to get segments with equal length
     * k ... Gradient
     * T ... Tangent
     * s ... Segment Length
     * <p>
     * T = (1, k)
     * <p>
     * Δx = (s * T / |T|).x
     * Δx = s * T.x / |T|
     * Δx = s * 1 / |T|
     * Δx = s / |T|
     * Δx = s / √(1^2 + k^2)
     * Δx = s / √(1 + k^2)
     *
     * @param s the desired segment length
     * @param k the gradient
     * @return Δx
     */
    private float estimateDeltaX(float s, float k) {
        return (float) (s / Math.sqrt(1 + k * k));
    }

    /**
     * Same as {@link #renderBaked(VertexConsumer, MatrixStack, BakeKey, Vec3f, int, int, int, int)} but will not use
     * the model cache. This should be used when {@code chainVec} is changed very frequently.
     *
     * @see #renderBaked
     */
    public void render(VertexConsumer buffer, MatrixStack matrices, Vec3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel model = buildModel(chainVec);
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
        private final int hash;

        public BakeKey(Vec3d srcPos, Vec3d dstPos) {
            float dY = (float) (srcPos.y - dstPos.y);
            float dXZ = Helper.distanceBetween(
                    new Vec3f((float) srcPos.x, 0, (float) srcPos.z),
                    new Vec3f((float) dstPos.x, 0, (float) dstPos.z));

            int hash = Float.floatToIntBits(dY);
            hash = 31 * hash + Float.floatToIntBits(dXZ);
            this.hash = hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BakeKey bakeKey = (BakeKey) o;
            return hash == bakeKey.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
