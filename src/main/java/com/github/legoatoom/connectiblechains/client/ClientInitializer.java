package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;

public class ClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(ConnectibleChains.CHAIN_KNOT,
                (entityRenderDispatcher, context) -> new ChainKnotEntityRenderer(entityRenderDispatcher));
    }
}
