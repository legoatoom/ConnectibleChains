package com.github.legoatoom.connectiblechains.chain;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * The 'material' of a chain
 */
public record ChainType(
        Identifier item,
        @Environment(EnvType.CLIENT) Identifier texture,
        @Environment(EnvType.CLIENT) Identifier knotTexture,
        @Environment(EnvType.CLIENT) UVRect uvSIdeA,
        @Environment(EnvType.CLIENT) UVRect uvSideB
) {
    public Item getItem() {
        return Registry.ITEM.get(item);
    }
}
