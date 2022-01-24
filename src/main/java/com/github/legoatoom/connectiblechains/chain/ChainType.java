package com.github.legoatoom.connectiblechains.chain;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * The 'material' of the chain
 */
public record ChainType(Identifier item, Identifier texture, Identifier knotTexture, UV uvSIdeA, UV uvSideB) {
    public Item getItem() {
        return Registry.ITEM.get(item);
    }
}
