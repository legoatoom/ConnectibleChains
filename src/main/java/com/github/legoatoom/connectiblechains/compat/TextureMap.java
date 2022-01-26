package com.github.legoatoom.connectiblechains.compat;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The connectible_chains_compat json file contains a map from item ids to pairs of knot and chain texture ids.
 */
public class TextureMap {
    @SuppressWarnings("CanBeFinal")
    public Map<String, Entry> textures = new HashMap<>();

    public Entry get(Identifier id) {
        return textures.get(id.toString());
    }

    public static class Entry {
        public String chain;
        public String knot;
    }
}
