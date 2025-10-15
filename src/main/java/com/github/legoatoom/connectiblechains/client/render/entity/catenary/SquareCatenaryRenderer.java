/*
 * Copyright (C) 2025 legoatoom
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity.catenary;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainModel;
import com.github.legoatoom.connectiblechains.client.render.entity.UVRect;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.github.legoatoom.connectiblechains.util.Helper.drip2;
import static com.github.legoatoom.connectiblechains.util.Helper.drip2prime;

public class SquareCatenaryRenderer extends CatenaryRenderer {
    public static final float SQRT_2 = (float) Math.sqrt(2);
    protected static final float CHAIN_SCALE = 1F;

    public SquareCatenaryRenderer(UVRect a, UVRect b) {
        super(a, b);
    }

    @Override
    public ChainModel buildModel(Vector3f chainVec) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        int initialCapacity = (int) (4f * chainVec.lengthSquared() / desiredSegmentLength);
        ChainModel.Builder builder = ChainModel.builder(initialCapacity);

        if (chainVec.x() == 0F && chainVec.z() == 0F) {
            buildFaceVertical(builder, chainVec);
        } else {
            buildFace(builder, chainVec);
        }

        return builder.build();
    }


    /**
     * {@link #buildFace} does not work when {@code endPosition} is pointing straight up or down.
     */
    private void buildFaceVertical(ChainModel.Builder builder, Vector3f endPosition) {
        endPosition.x = 0F;
        endPosition.z = 0F;
        final float chainHalfWidthA = (SIDE_A.x1() - SIDE_A.x0()) / 32F * CHAIN_SCALE;
        final float chainHalfWidthB = (SIDE_B.x1() - SIDE_B.x0()) / 32F * CHAIN_SCALE;

        Vector3f normalA = new Vector3f((float) Math.cos(Math.toRadians(45)), 0F, (float) Math.sin(Math.toRadians(45)));
        Vector3f normalB = new Vector3f((float) Math.cos(Math.toRadians(-45)), 0F, (float) Math.sin(Math.toRadians(-45)));
        normalA.normalize(chainHalfWidthA * SQRT_2);
        normalB.normalize(chainHalfWidthB * SQRT_2);

        Vector3f vert00A = new Vector3f(-normalA.x(), 0, -normalA.z());
        Vector3f vert01A = new Vector3f(normalA.x(), 0, normalA.z());
        Vector3f vert10A = new Vector3f(-normalA.x(), endPosition.y(), -normalA.z());
        Vector3f vert11A = new Vector3f(normalA.x(), endPosition.y(), normalA.z());

        Vector3f vert00B = new Vector3f(-normalB.x(), 0, -normalB.z());
        Vector3f vert01B = new Vector3f(normalB.x(), 0, normalB.z());
        Vector3f vert10B = new Vector3f(-normalB.x(), endPosition.y(), -normalB.z());
        Vector3f vert11B = new Vector3f(normalB.x(), endPosition.y(), normalB.z());

        float uvv0 = 0F, uvv1 = Math.abs(endPosition.y()) / CHAIN_SCALE;
        build4Sides(builder, uvv0, uvv1, vert00A, vert01A, vert10A, vert11A, vert00B, vert01B, vert10B, vert11B);
    }

    /**
     * Creates geometry from the origin to {@code endPosition} with the specified {@code angle}.
     * It uses an iterative approach meaning that it adds geometry until it's at the end or
     * has reached {@link #MAX_SEGMENTS}.
     * The model is always generated along the local X axis and curves along the Y axis.
     * This makes the calculation a lot simpler as we are only dealing with 2d coordinates.
     *
     * @param builder     The target builder
     * @param endPosition The end position in relation to the origin
     */
    private void buildFace(ChainModel.Builder builder, Vector3f endPosition) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        // Distance XYZ
        float distance = endPosition.length();
        // Distance XZ
        float distanceXZ = (float) Math.sqrt(Math.fma(endPosition.x(), endPosition.x(), endPosition.z() * endPosition.z()));
        // Original code used total distance between start and end instead of horizontal distance
        // That changed the look of chains when there was a big height difference, but it looks better.
        final float wrongDistanceFactor = distance / distanceXZ;
        final float chainHalfWidthA = (SIDE_A.x1() - SIDE_A.x0()) / 32F * CHAIN_SCALE;
        final float chainHalfWidthB = (SIDE_B.x1() - SIDE_B.x0()) / 32F * CHAIN_SCALE;
        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();
        // 00, 01, 11, 11 refers to the X and Y position of the vertex.
        // 00 is the lower X and Y vertex. 10 Has the same y value as 00 but a higher x value.
        Vector3f vert00A = new Vector3f();
        Vector3f vert01A = new Vector3f();
        Vector3f vert11A = new Vector3f();
        Vector3f vert10A = new Vector3f();
        Vector3f vert00B = new Vector3f();
        Vector3f vert01B = new Vector3f();
        Vector3f vert11B = new Vector3f();
        Vector3f vert10B = new Vector3f();
        Quaternionf rotatorA = new Quaternionf();
        Quaternionf rotatorB = new Quaternionf();
        Vector3f segmentStart = new Vector3f(), segmentEnd = new Vector3f();

        float uvv1 = 0;
        float uvv0;
        float x = 0;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            float gradient = (float) drip2prime(x * wrongDistanceFactor, distance, endPosition.y());
            x += estimateDeltaX(desiredSegmentLength, gradient);
            x = Math.min(distanceXZ, x);
            float y = (float) drip2(x * wrongDistanceFactor, distance, endPosition.y());
            segmentEnd.set(x, y, 0);

            rotAxis.set(segmentEnd.x() - segmentStart.x(), segmentEnd.y() - segmentStart.y(), segmentEnd.z() - segmentStart.z());
            rotAxis.normalize();
            rotatorA = rotatorA.fromAxisAngleDeg(rotAxis, -45);
            rotatorB = rotatorB.fromAxisAngleDeg(rotAxis, 45);

            // This normal is orthogonal to the face normal
            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            Vector3f normalA = new Vector3f(), normalB = new Vector3f();
            normal.rotate(rotatorA, normalA);
            normal.rotate(rotatorB, normalB);
            normalA.normalize(chainHalfWidthA * SQRT_2);
            normalB.normalize(chainHalfWidthB * SQRT_2);

            if (segment == 0) {
                //first iteration, thus the previous one does not yet exist.
                vert00A.set(segmentStart).sub(normalA);
                vert01A.set(segmentStart).add(normalA);
                vert00B.set(segmentStart).sub(normalB);
                vert01B.set(segmentStart).add(normalB);
            } else {
                vert00A.set(vert10A);
                vert01A.set(vert11A);
                vert00B.set(vert10B);
                vert01B.set(vert11B);
            }
            vert10A.set(segmentEnd).sub(normalA);
            vert11A.set(segmentEnd).add(normalA);
            vert10B.set(segmentEnd).sub(normalB);
            vert11B.set(segmentEnd).add(normalB);

            float actualSegmentLength = segmentStart.distance(segmentEnd);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            build4Sides(builder, uvv0, uvv1, vert00A, vert01A, vert10A, vert11A, vert00B, vert01B, vert10B, vert11B);

            if (x >= distanceXZ) {
                break;
            }
            segmentStart.set(segmentEnd);
        }
    }

    private void build4Sides(ChainModel.Builder builder, float uvv0, float uvv1, Vector3f vert00A, Vector3f vert01A, Vector3f vert10A, Vector3f vert11A, Vector3f vert00B, Vector3f vert01B, Vector3f vert10B, Vector3f vert11B) {
        builder.vertex(vert10A).uv(SIDE_A.x1() / 16f, uvv1).next();
        builder.vertex(vert11B).uv(SIDE_A.x0() / 16f, uvv1).next();
        builder.vertex(vert01B).uv(SIDE_A.x0() / 16f, uvv0).next();
        builder.vertex(vert00A).uv(SIDE_A.x1() / 16f, uvv0).next();

        builder.vertex(vert00A).uv(SIDE_B.x0() / 16f, uvv0).next();
        builder.vertex(vert00B).uv(SIDE_B.x1() / 16f, uvv0).next();
        builder.vertex(vert10B).uv(SIDE_B.x1() / 16f, uvv1).next();
        builder.vertex(vert10A).uv(SIDE_B.x0() / 16f, uvv1).next();

        builder.vertex(vert00B).uv(SIDE_A.x1() / 16f, uvv0).next();
        builder.vertex(vert01A).uv(SIDE_A.x0() / 16f, uvv0).next();
        builder.vertex(vert11A).uv(SIDE_A.x0() / 16f, uvv1).next();
        builder.vertex(vert10B).uv(SIDE_A.x1() / 16f, uvv1).next();

        builder.vertex(vert01A).uv(SIDE_B.x0() / 16f, uvv0).next();
        builder.vertex(vert01B).uv(SIDE_B.x1() / 16f, uvv0).next();
        builder.vertex(vert11B).uv(SIDE_B.x1() / 16f, uvv1).next();
        builder.vertex(vert11A).uv(SIDE_B.x0() / 16f, uvv1).next();
    }
}
