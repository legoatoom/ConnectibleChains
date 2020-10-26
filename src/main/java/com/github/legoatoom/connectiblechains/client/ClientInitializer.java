package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.entity.Entity;

public class ClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(ModEntityTypes.CHAIN_KNOT,
                (entityRenderDispatcher, context) -> new ChainKnotEntityRenderer(entityRenderDispatcher));

        ClientSidePacketRegistry.INSTANCE.register(NetworkingPackages.S2C_CHAIN_ATTACH_PACKET_ID,
                ((packetContext, packetByteBuf) -> {
                    int[] fromTo = packetByteBuf.readIntArray();
                    int fromPlayer = packetByteBuf.readInt();
                    packetContext.getTaskQueue().execute(() -> {
                        Entity entity = packetContext.getPlayer().world.getEntityById(fromTo[0]);
                        if (entity instanceof ChainKnotEntity){
                            ((ChainKnotEntity) entity).addHoldingEntityId(fromTo[1], fromPlayer);
                        }
                    });
                }));

        ClientSidePacketRegistry.INSTANCE.register(NetworkingPackages.S2C_CHAIN_DETACH_PACKET_ID,
                ((packetContext, packetByteBuf) -> {
                    int[] fromTo = packetByteBuf.readIntArray();
                    packetContext.getTaskQueue().execute(() -> {
                        Entity entity = packetContext.getPlayer().world.getEntityById(fromTo[0]);
                        if (entity instanceof ChainKnotEntity){
                            ((ChainKnotEntity) entity).removeHoldingEntityId(fromTo[1]);
                        }
                    });
                }));

        ClientSidePacketRegistry.INSTANCE.register(NetworkingPackages.S2C_MULTI_CHAIN_ATTACH_PACKET_ID,
                ((packetContext, packetByteBuf) -> {
                    int from = packetByteBuf.readInt();
                    int[] tos = packetByteBuf.readIntArray();
                    packetContext.getTaskQueue().execute(() -> {
                        Entity entity = packetContext.getPlayer().world.getEntityById(from);
                        if (entity instanceof ChainKnotEntity){
                            ((ChainKnotEntity) entity).addHoldingEntityIds(tos);
                        }
                    });
                }));

        ClientSidePacketRegistry.INSTANCE.register(NetworkingPackages.S2C_MULTI_CHAIN_DETACH_PACKET_ID,
                ((packetContext, packetByteBuf) -> {
                    int from = packetByteBuf.readInt();
                    int[] tos = packetByteBuf.readIntArray();
                    packetContext.getTaskQueue().execute(() -> {
                        Entity entity = packetContext.getPlayer().world.getEntityById(from);
                        if (entity instanceof ChainKnotEntity){
                            ((ChainKnotEntity) entity).removeHoldingEntityIds(tos);
                        }
                    });
                }));
    }
}
