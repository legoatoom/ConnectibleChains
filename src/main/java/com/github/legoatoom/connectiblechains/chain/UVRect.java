package com.github.legoatoom.connectiblechains.chain;

/**
 * Specifies the u coordinates that the renderer should use.
 * The chain texture has to be vertical for now.
 */
public record UVRect(float x0, float x1) {
    public static final UVRect DEFAULT_SIDE_A = new UVRect(0, 3);
    public static final UVRect DEFAULT_SIDE_B = new UVRect(3, 6);
}
