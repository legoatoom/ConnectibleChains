package com.github.legoatoom.connectiblechains.client.render.entity.catenary;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainModel;
import com.github.legoatoom.connectiblechains.client.render.entity.UVRect;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static com.github.legoatoom.connectiblechains.util.Helper.drip2;
import static com.github.legoatoom.connectiblechains.util.Helper.drip2prime;

public class PlussCatenaryRenderer extends CatenaryRenderer {

    @Override
    public ChainModel buildModel(Vector3f chainVec) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        int initialCapacity = (int) (2f * chainVec.lengthSquared() / desiredSegmentLength);
        ChainModel.Builder builder = ChainModel.builder(initialCapacity);

        if (chainVec.x() == 0F && chainVec.z() == 0F) {
            buildFaceVertical(builder, chainVec, 0, UVRect.DEFAULT_SIDE_A);
            buildFaceVertical(builder, chainVec, 90, UVRect.DEFAULT_SIDE_B);
        } else {
            buildFace(builder, chainVec, 0, UVRect.DEFAULT_SIDE_A);
            buildFace(builder, chainVec, 90, UVRect.DEFAULT_SIDE_B);
        }

        return builder.build();
    }


    /**
     * {@link #buildFace} does not work when {@code endPosition} is pointing straight up or down.
     */
    private void buildFaceVertical(ChainModel.Builder builder, Vector3f endPosition, float angle, UVRect uv) {
        endPosition.x = 0F;
        endPosition.z = 0F;
        final float chainWidth = (uv.x1() - uv.x0()) / 16F * CHAIN_SCALE;

        Vector3f normal = new Vector3f((float) Math.cos(Math.toRadians(angle)), 0F, (float) Math.sin(Math.toRadians(angle)));
        normal.normalize(chainWidth / 2);

        Vector3f vert00 = new Vector3f(-normal.x(), 0, -normal.z());
        Vector3f vert01 = new Vector3f(normal.x(), 0, normal.z());
        Vector3f vert10 = new Vector3f(-normal.x(), endPosition.y(), -normal.z());
        Vector3f vert11 = new Vector3f(normal.x(), endPosition.y(), normal.z());

        float uvv0 = 0F, uvv1 = Math.abs(endPosition.y()) / CHAIN_SCALE;
        builder.vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
        builder.vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
        builder.vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
        builder.vertex(vert10).uv(uv.x0() / 16f, uvv1).next();
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
     * @param angle       The angle of the face
     * @param uv          The uv bounds of the face
     */
    private void buildFace(ChainModel.Builder builder, Vector3f endPosition, float angle, UVRect uv) {
        float desiredSegmentLength = 1f / ConnectibleChains.runtimeConfig.getQuality();
        // Distance XYZ
        float distance = endPosition.length();
        // Distance XZ
        float distanceXZ = (float) Math.sqrt(Math.fma(endPosition.x(), endPosition.x(), endPosition.z() * endPosition.z()));
        // Original code used total distance between start and end instead of horizontal distance
        // That changed the look of chains when there was a big height difference, but it looks better.
        final float wrongDistanceFactor = distance / distanceXZ;
        final float chainWidth = (uv.x1() - uv.x0()) / 16F * CHAIN_SCALE;
        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();
        // 00, 01, 11, 11 refers to the X and Y position of the vertex.
        // 00 is the lower X and Y vertex. 10 Has the same y value as 00 but a higher x value.
        Vector3f vert00 = new Vector3f();
        Vector3f vert01 = new Vector3f();
        Vector3f vert11 = new Vector3f();
        Vector3f vert10 = new Vector3f();
        Quaternionf rotator = new Quaternionf();
        Vector3f segmentStart = new Vector3f(), segmentEnd = new Vector3f();

        float uvv1 = 0;
        float uvv0;
        float x = 0;
        for (int segment = 0; segment < MAX_SEGMENTS; segment++) {
            float gradient = (float) drip2prime(x * wrongDistanceFactor, distance, endPosition.y());
            x += estimateDeltaX(desiredSegmentLength, gradient);
            x = Math.min(x, distanceXZ);

            float y = (float) drip2(x * wrongDistanceFactor, distance, endPosition.y());
            segmentEnd.set(x, y, 0);

            rotAxis.set(segmentEnd.x() - segmentStart.x(), segmentEnd.y() - segmentStart.y(), segmentEnd.z() - segmentStart.z());
            rotAxis.normalize();
            rotator = rotator.fromAxisAngleDeg(rotAxis, angle);

            // This normal is orthogonal to the face normal
            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotate(rotator);
            normal.normalize(chainWidth / 2);

            if (segment == 0) {
                //first iteration, thus the previous one does not yet exist.
                vert00.set(segmentStart).sub(normal);
                vert01.set(segmentStart).add(normal);
            } else {
                vert00.set(vert10);
                vert01.set(vert11);
            }
            vert10.set(segmentEnd).sub(normal);
            vert11.set(segmentEnd).add(normal);

            float actualSegmentLength = segmentStart.distance(segmentEnd);

            uvv0 = uvv1;
            uvv1 = uvv0 + actualSegmentLength / CHAIN_SCALE;

            builder.vertex(vert00).uv(uv.x0() / 16f, uvv0).next();
            builder.vertex(vert01).uv(uv.x1() / 16f, uvv0).next();
            builder.vertex(vert11).uv(uv.x1() / 16f, uvv1).next();
            builder.vertex(vert10).uv(uv.x0() / 16f, uvv1).next();

            if (x >= distanceXZ) {
                break;
            }
            segmentStart.set(segmentEnd);
        }
    }
}
