package com.legoatoom.develop.client;

import com.legoatoom.develop.ConnectibleChains;
import com.legoatoom.develop.client.render.entity.ChainKnotEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;

public class ClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(ConnectibleChains.CHAIN_KNOT,
                (entityRenderDispatcher, context) -> new ChainKnotEntityRenderer(entityRenderDispatcher));
    }
}
