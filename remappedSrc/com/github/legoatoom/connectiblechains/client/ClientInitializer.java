/*
 * Copyright (C) 2021 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.github.legoatoom.connectiblechains.enitity.ChainCollisionEntity;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import com.github.legoatoom.connectiblechains.util.PacketBufUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;

import java.util.UUID;

/**
 * ClientInitializer.
 * This method is called when the game starts with a client.
 * This registers the renderers for entities and how to handle packages between the server and client.
 *
 * @author legoatoom
 */
public class ClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        initRenderers();
        registerReceiverClientPackages();
    }

    private void initRenderers() {
        EntityRendererRegistry.INSTANCE.register(ModEntityTypes.CHAIN_KNOT,
                (entityRenderDispatcher, context) -> new ChainKnotEntityRenderer(context));

        EntityRendererRegistry.INSTANCE.register(ModEntityTypes.CHAIN_COLLISION,
                (entityRenderDispatcher, context) -> new ChainCollisionEntityRenderer(context));
    }

    private void registerReceiverClientPackages() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackages.S2C_CHAIN_ATTACH_PACKET_ID,
                (client, handler, packetByteBuf, responseSender) -> {
                    int[] fromTo = packetByteBuf.readIntArray();
                    int fromPlayer = packetByteBuf.readInt();
                    client.execute(() -> {
                        if (client.world != null) {
                            Entity entity = client.world.getEntityById(fromTo[0]);
                            if (entity instanceof ChainKnotEntity) {
                                ((ChainKnotEntity) entity).addHoldingEntityId(fromTo[1], fromPlayer);
                            } else {
                                LogManager.getLogger().warn("Received Attach Chain Package to unknown Entity.");
                            }
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackages.S2C_CHAIN_DETACH_PACKET_ID,
                (client, handler, packetByteBuf, responseSender) -> {
                    int[] fromTo = packetByteBuf.readIntArray();
                    client.execute(() -> {
                        if (client.world != null) {
                            Entity entity = client.world.getEntityById(fromTo[0]);
                            if (entity instanceof ChainKnotEntity) {
                                ((ChainKnotEntity) entity).removeHoldingEntityId(fromTo[1]);
                            }
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackages.S2C_MULTI_CHAIN_ATTACH_PACKET_ID,
                (client, handler, packetByteBuf, responseSender) -> {
                    int from = packetByteBuf.readInt();
                    int[] tos = packetByteBuf.readIntArray();
                    client.execute(() -> {
                        if (client.world != null) {
                            Entity entity = client.world.getEntityById(from);
                            if (entity instanceof ChainKnotEntity) {
                                ((ChainKnotEntity) entity).addHoldingEntityIds(tos);
                            }
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackages.S2C_SPAWN_CHAIN_COLLISION_PACKET,
                (client, handler, buf, responseSender) -> {
                    int entityTypeID = buf.readVarInt();
                    EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeID);
                    UUID uuid = buf.readUuid();
                    int entityId = buf.readVarInt();
                    Vec3d pos = PacketBufUtil.readVec3d(buf);
                    float pitch = PacketBufUtil.readAngle(buf);
                    float yaw = PacketBufUtil.readAngle(buf);

                    int startId = buf.readVarInt();
                    int endId = buf.readVarInt();

                    client.execute(() -> {
                        if (MinecraftClient.getInstance().world == null){
                            throw new IllegalStateException("Tried to spawn entity in a null world!");
                        }
                        Entity e = entityType.create(MinecraftClient.getInstance().world);
                        if (e == null){
                            throw new IllegalStateException("Failed to create instance of entity \"" + entityTypeID + "\"");
                        }
//                        e.updateTrackedPosition(pos);
                        e.setPosition(pos.x, pos.y, pos.z);
                        e.pitch = pitch;
                        e.yaw = yaw;
                        e.setEntityId(entityId);
                        e.setUuid(uuid);
                        e.setVelocity(Vec3d.ZERO);
                        if (e instanceof ChainCollisionEntity){

                            ((ChainCollisionEntity) e).setStartOwnerId(startId);
                            ((ChainCollisionEntity) e).setEndOwnerId(endId);
                            e.setBoundingBox(new Box(pos, pos).expand(.01d, 0, .01d));
                        }
                        MinecraftClient.getInstance().world.addEntity(entityId, e);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackages.S2C_SPAWN_CHAIN_KNOT_PACKET,
                (client, handler, buf, responseSender) -> {
                    int entityTypeID = buf.readVarInt();
                    EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeID);
                    UUID uuid = buf.readUuid();
                    int entityId = buf.readVarInt();
                    Vec3d pos = PacketBufUtil.readVec3d(buf);
                    float pitch = PacketBufUtil.readAngle(buf);
                    float yaw = PacketBufUtil.readAngle(buf);

                    client.execute(() -> {
                        if (MinecraftClient.getInstance().world == null){
                            throw new IllegalStateException("Tried to spawn entity in a null world!");
                        }
                        Entity e = entityType.create(MinecraftClient.getInstance().world);
                        if (e == null){
                            throw new IllegalStateException("Failed to create instance of entity \"" + entityTypeID + "\"");
                        }
//                        e.updateTrackedPosition(pos);
                        e.setPosition(pos.x, pos.y, pos.z);
                        e.setPitch(pitch);
                        e.setYaw(yaw);
                        e.setId(entityId);
                        e.setUuid(uuid);
                        e.setVelocity(Vec3d.ZERO);
                        if (e instanceof ChainKnotEntity){
                            e.setBoundingBox(new Box(pos.getX() - 0.1875D, pos.getY() - 0.25D + 0.125D, pos.getZ() - 0.1875D,
                                    pos.getX() + 0.1875D, pos.getY() + 0.25D + 0.125D, pos.getZ() + 0.1875D));
                            e.teleporting = true;
                        }
                        MinecraftClient.getInstance().world.addEntity(entityId, e);
                    });
                });

        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (result instanceof EntityHitResult){
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof ChainKnotEntity || entity instanceof ChainCollisionEntity){
                    return new ItemStack(Items.CHAIN);
                }
            }
            return ItemStack.EMPTY;
        });

    }
}
