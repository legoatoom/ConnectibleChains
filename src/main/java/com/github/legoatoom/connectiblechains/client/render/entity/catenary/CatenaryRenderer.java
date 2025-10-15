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

import com.github.legoatoom.connectiblechains.client.render.entity.ChainModel;
import com.github.legoatoom.connectiblechains.client.render.entity.UVRect;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.function.BiFunction;

public abstract class CatenaryRenderer {

    /**
     * Changes the width of the chain but does not cause uv distortion.
     */
    protected static final float CHAIN_SCALE = 1f;
    /**
     * How many mesh segments a chain is allowed to have.
     * This is to prevent extreme lag and the possibility of an infinite loop.
     */
    protected static final int MAX_SEGMENTS = 2048;

    private static final HashMap<Identifier, BiFunction<UVRect, UVRect, CatenaryRenderer>> renderers = new HashMap<>();
    protected final UVRect SIDE_A;
    protected final UVRect SIDE_B;

    protected CatenaryRenderer(UVRect a, UVRect b) {
        SIDE_A = a;
        SIDE_B = b;
    }

    public static void addRenderer(Identifier id, BiFunction<UVRect, UVRect, CatenaryRenderer> rendererSupplier) {
        renderers.put(id, rendererSupplier);
    }

    /**
     * Get the renderer for a given id. If non exist, get the {@link CrossCatenaryRenderer} as default.
     *
     * @param id The identifier of the renderer.
     * @param uvRects The UV mapping to use, determine the width.
     * @return a ICatenaryRenderer.
     */
    public static CatenaryRenderer getRenderer(Identifier id, Pair<UVRect, UVRect> uvRects) {
        return renderers.getOrDefault(id, CrossCatenaryRenderer::new).apply(uvRects.getLeft(), uvRects.getRight());
    }

    /**
     * Generates a new baked chain model for the given vector.
     *
     * @param chainVec The vector from the chain start to the end
     * @return The generated model
     */
    public abstract ChainModel buildModel(Vector3f chainVec);

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
    protected float estimateDeltaX(float s, float k) {
        return (float) (s / Math.sqrt(1 + k * k));
    }
}
