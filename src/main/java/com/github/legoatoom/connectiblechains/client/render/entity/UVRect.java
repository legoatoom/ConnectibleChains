package com.github.legoatoom.connectiblechains.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Specifies the uv coordinates that the renderer should use.
 * The chain texture has to be vertical for now.
 *
 * @implNote This is a leftover and serves no real function
 */
@Environment(EnvType.CLIENT)
public record UVRect(float x0, float x1) {
    /**
     * Default UV's for side A
     */
    public static final UVRect DEFAULT_SIDE_A = new UVRect(0, 3);
    /**
     * Default UV's for side B
     */
    public static final UVRect DEFAULT_SIDE_B = new UVRect(3, 6);
}
