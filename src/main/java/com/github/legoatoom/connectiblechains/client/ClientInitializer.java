package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.entity.Entity;

public class ClientInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(ConnectibleChains.CHAIN_KNOT,
                (entityRenderDispatcher, context) -> new ChainKnotEntityRenderer(entityRenderDispatcher));

        ClientSidePacketRegistry.INSTANCE.register(NetworkingPackages.S2C_CHAIN_ATTACH_PACKET_ID,
                ((packetContext, packetByteBuf) -> {
                    int[] fromTo = packetByteBuf.readIntArray();
                    packetContext.getTaskQueue().execute(() -> {
                        Entity entity = packetContext.getPlayer().world.getEntityById(fromTo[0]);
                        if (entity instanceof ChainKnotEntity){
                            ((ChainKnotEntity) entity).setHoldingEntityId(fromTo[1]);
                        }
                    });
                }));
    }
}
