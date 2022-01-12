package com.github.legoatoom.connectiblechains.client.render.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

import static com.github.legoatoom.connectiblechains.util.Helper.drip2;
import static com.github.legoatoom.connectiblechains.util.Helper.drip2prime;

public class ChainRenderer {
    private final Object2ObjectOpenHashMap<BakeKey, ChainModel.BakedChainModel> models = new Object2ObjectOpenHashMap<>(256);
    private static final float CHAIN_SCALE = 1f;
    private static final float CHAIN_SIZE = CHAIN_SCALE * 3/16f;
    private static final int MAX_SEGMENTS = 2048;

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
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BakeKey bakeKey = (BakeKey) o;

            return hash == bakeKey.hash;
        }
    }

    public void renderBaked(VertexConsumer buffer, MatrixStack matrices, BakeKey key, Vec3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel.BakedChainModel model;
        if(models.containsKey(key)) {
            model = models.get(key);
        } else {
            model = bakeModel(chainVec);
            models.put(key, model);
        }
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    public void render(VertexConsumer buffer, MatrixStack matrices, Vec3f chainVec, int blockLight0, int blockLight1, int skyLight0, int skyLight1) {
        ChainModel.BakedChainModel model = bakeModel(chainVec);
        model.render(buffer, matrices, blockLight0, blockLight1, skyLight0, skyLight1);
    }

    private ChainModel.BakedChainModel bakeModel(Vec3f chainVec) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        int initialCapacity = (int) (2f * Helper.lengthOf(chainVec) / desiredSegmentLength);
        ChainModel model = new ChainModel(initialCapacity);

        if(chainVec.getX() == 0 && chainVec.getZ() == 0) {
            buildFaceVertical(model, chainVec, 45, 0);
            buildFaceVertical(model, chainVec, -45, 3);
        } else {
            buildFace(model, chainVec, 45, 0);
            buildFace(model, chainVec, -45, 3);
        }

        return model.bake();
    }

    private void buildFaceVertical(ChainModel model, Vec3f v, float angle, int uvu) {
        float actualSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        Vec3f normal = new Vec3f((float)Math.cos(Math.toRadians(angle)), 0, (float)Math.sin(Math.toRadians(angle)));
        normal.scale(CHAIN_SIZE);

        Vec3f vert00 = new Vec3f(-normal.getX()/2, 0, -normal.getZ()/2), vert01 = vert00.copy();
        vert01.add(normal);
        Vec3f vert10 = new Vec3f(-normal.getX()/2, 0, -normal.getZ()/2), vert11 = vert10.copy();
        vert11.add(normal);

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter_ = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            if(vert00.getY() + actualSegmentLength >= v.getY()) {
                lastIter_ = true;
                actualSegmentLength = v.getY() - vert00.getY();
            }

            vert10.add(0, actualSegmentLength, 0);
            vert11.add(0, actualSegmentLength, 0);

            uvv1 += actualSegmentLength / CHAIN_SCALE;

            model.vertex(vert00).uv(uvu/16f, uvv0).next();
            model.vertex(vert01).uv((uvu+3)/16f, uvv0).next();
            model.vertex(vert11).uv((uvu+3)/16f, uvv1).next();
            model.vertex(vert10).uv(uvu/16f, uvv1).next();

            if(lastIter_) break;

            uvv0 = uvv1;

            vert00.set(vert10);
            vert01.set(vert11);
        }
    }

    private void buildFace(ChainModel model, Vec3f v, float angle, int uvu) {
        float actualSegmentLength, desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        float distance = Helper.lengthOf(v), distanceXZ = (float) Math.sqrt(v.getX()*v.getX() + v.getZ()*v.getZ());
        // Original code used total distance between start and end instead of horizontal distance
        // That changed the look of chains when there was a big height difference, but it looks better.
        float wrongDistanceFactor = distance/distanceXZ;

        Vec3f vert00 = new Vec3f(), vert01 = new Vec3f(), vert11 = new Vec3f(), vert10 = new Vec3f();
        Vec3f normal = new Vec3f(), rotAxis = new Vec3f();

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
        gradient = (float) drip2prime(x*wrongDistanceFactor, distance, v.getY());
        y = (float) drip2(x*wrongDistanceFactor, distance, v.getY());
        point1.set(x, y, 0);

        rotAxis.set(point1.getX() - point0.getX(), point1.getY() - point0.getY(), point1.getZ() - point0.getZ());
        rotAxis.normalize();
        rotator = rotAxis.getDegreesQuaternion(angle);

        normal.rotate(rotator);
        normal.scale(CHAIN_SIZE);
        vert10.set(point0.getX() - normal.getX()/2, point0.getY() - normal.getY()/2, point0.getZ() - normal.getZ()/2);
        vert11.set(vert10);
        vert11.add(normal);

        actualSegmentLength = Helper.distanceBetween(point0, point1);

        boolean lastIter_ = false;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            rotAxis.set(point1.getX() - point0.getX(), point1.getY() - point0.getY(), point1.getZ() - point0.getZ());
            rotAxis.normalize();
            rotator = rotAxis.getDegreesQuaternion(angle);

            // This normal is orthogonal to the face normal
            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotate(rotator);
            normal.scale(CHAIN_SIZE);

            vert00.set(vert10);
            vert01.set(vert11);

            vert10.set(point1.getX() - normal.getX()/2, point1.getY() - normal.getY()/2, point1.getZ() - normal.getZ()/2);
            vert11.set(vert10);
            vert11.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            model.vertex(vert00).uv(uvu/16f, uvv0).next();
            model.vertex(vert01).uv((uvu+3)/16f, uvv0).next();
            model.vertex(vert11).uv((uvu+3)/16f, uvv1).next();
            model.vertex(vert10).uv(uvu/16f, uvv1).next();

            if(lastIter_) break;

            point0.set(point1);

            x += estimateDeltaX(desiredSegmentLength, gradient);
            if(x >= distanceXZ) {
                lastIter_ = true;
                x = distanceXZ;
            }

            gradient = (float) drip2prime(x*wrongDistanceFactor, distance, v.getY());
            y = (float) drip2(x*wrongDistanceFactor, distance, v.getY());
            point1.set(x, y, 0);

            actualSegmentLength = Helper.distanceBetween(point0, point1);
        }
    }

    /**
     * Estimate Δx based on current gradient to get segments with equal length
     * k ... Gradient
     * T ... Tangent
     * s ... Segment Length
     *
     * T = (1, k)
     *
     * Δx = (s * T / |T|).x
     * Δx = s * T.x / |T|
     * Δx = s * 1 / |T|
     * Δx = s / |T|
     * Δx = s / √(1^2 + k^2)
     * Δx = s / √(1 + k^2)
     * @param s the desired segment length
     * @param k the gradient
     * @return Δx
     */
    private float estimateDeltaX(float s, float k) {
        return (float) (s / Math.sqrt(1 + k*k));
    }

    public void purge() {
        models.clear();
    }
}
