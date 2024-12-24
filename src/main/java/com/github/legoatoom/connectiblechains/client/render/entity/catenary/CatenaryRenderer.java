package com.github.legoatoom.connectiblechains.client.render.entity.catenary;

import com.github.legoatoom.connectiblechains.client.render.entity.ChainModel;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.function.Supplier;

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

    private static final HashMap<Identifier, Supplier<CatenaryRenderer>> renderers = new HashMap<>();

    public static void addRenderer(Identifier id, Supplier<CatenaryRenderer> rendererSupplier) {
        renderers.put(id, rendererSupplier);
    }

    /**
     * Get the renderer for a given id. If non exist, get the {@link CrossCatenaryRenderer} as default.
     *
     * @param id The identifier of the renderer.
     * @return a ICatenaryRenderer.
     */
    public static CatenaryRenderer getRenderer(Identifier id) {
        return renderers.getOrDefault(id, CrossCatenaryRenderer::new).get();
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
