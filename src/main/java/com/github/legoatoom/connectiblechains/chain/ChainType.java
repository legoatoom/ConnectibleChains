package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

/**
 * The 'material' of a chain
 */
public record ChainType(Item item) {
    @Environment(EnvType.CLIENT)
    public Identifier getKnotTexture() {
        return ClientInitializer.getInstance().textureManager.getKnotTexture(ChainTypesRegistry.REGISTRY.getId(this));
    }

    @Environment(EnvType.CLIENT)
    public Identifier getChainTexture() {
        return ClientInitializer.getInstance().textureManager.getChainTexture(ChainTypesRegistry.REGISTRY.getId(this));
    }
}
