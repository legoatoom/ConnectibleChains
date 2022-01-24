package com.github.legoatoom.connectiblechains.chain;

/**
 * Specifies the u coordinates that the renderer should use.
 * The chain texture has to be vertical for now.
 */
public record UV(float x0, float x1) {
    public static final UV DEFAULT_SIDE_A = new UV(0, 3);
    public static final UV DEFAULT_SIDE_B = new UV(3, 6);
}
