package com.github.legoatoom.connectiblechains.server;

import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class ServerInitializer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        // ClientInitializer uses CLIENT_STARTED
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> ChainTypesRegistry.lock());
    }
}
