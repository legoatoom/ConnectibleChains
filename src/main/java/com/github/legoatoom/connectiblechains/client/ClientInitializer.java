/*
 * Copyright (C) 2022 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.chain.IncompleteChainLink;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.compat.ChainTypes;
import com.github.legoatoom.connectiblechains.compat.SidedResourceReloadListener;
import com.github.legoatoom.connectiblechains.config.ModConfig;
import com.github.legoatoom.connectiblechains.enitity.ChainCollisionEntity;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import com.github.legoatoom.connectiblechains.util.PacketBufUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.UUID;

/**
 * ClientInitializer.
 * This method is called when the game starts with a client.
 * This registers the renderers for entities and how to handle packages between the server and client.
 *
 * @author legoatoom
 */
public class ClientInitializer implements ClientModInitializer {

    public static final EntityModelLayer CHAIN_KNOT = new EntityModelLayer(Helper.identifier("chain_knot"), "main");
    public static ChainTypes types = new ChainTypes();
    private static ChainKnotEntityRenderer chainKnotEntityRenderer = null;
    private static ClientInitializer instance;

    /**
     * Links where this is the primary and the secondary doesn't yet exist / hasn't yet loaded.
     * They are kept in a separate list to prevent accidental accesses of the secondary which would
     * result in a NPE. The links will try to be completed each world tick.
     */
    private static final ObjectList<IncompleteChainLink> INCOMPLETE_LINKS = new ObjectArrayList<>(256);

//    /**
//     * Collision entities which don't have a complete link yet.
//     * Their links will try to be set each world tick.
//     */
//    private static final Long2ObjectMap<ObjectList<ChainCollisionEntity>> INCOMPLETE_COLLIDERS = new Long2ObjectOpenHashMap<>(256);

    @Override
    public void onInitializeClient() {
        instance = this;
        initRenderers();
        registerReceiverClientPackages();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                SidedResourceReloadListener.proxy(ResourceType.CLIENT_RESOURCES, types));

        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        configHolder.registerSaveListener((holder, modConfig) -> {
            ClientInitializer clientInitializer = ClientInitializer.getInstance();
            if (clientInitializer != null) {
                clientInitializer.getChainKnotEntityRenderer().getChainRenderer().purge();
            }
            MinecraftServer server = MinecraftClient.getInstance().getServer();
            if (server != null) {
                ConnectibleChains.LOGGER.info("Syncing config to clients");
                ConnectibleChains.fileConfig.syncToClients(server);
                ConnectibleChains.runtimeConfig.copyFrom(ConnectibleChains.fileConfig);
            }
            return ActionResult.PASS;
        });
    }

    private void initRenderers() {
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_KNOT, ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_COLLISION,
                ChainCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    private void registerReceiverClientPackages() {
//        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_CHAIN_ATTACH_PACKET_ID,
//                (client, handler, packetByteBuf, responseSender) -> {
//                    int[] fromTo = packetByteBuf.readIntArray();
//                    int fromPlayer = packetByteBuf.readInt();
//                    client.execute(() -> {
//                        if (client.world != null) {
//                            Entity entity = client.world.getEntityById(fromTo[0]);
//                            if (entity instanceof ChainKnotEntity knot) {
//                                knot.addHoldingEntityId(fromTo[1], fromPlayer);
//                            } else {
//                                ConnectibleChains.LOGGER.warn("Received Attach Chain Package to unknown Entity.");
//                            }
//                        }
//                    });
//                });
//
//        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_CHAIN_DETACH_PACKET_ID,
//                (client, handler, packetByteBuf, responseSender) -> {
//                    int[] fromTo = packetByteBuf.readIntArray();
//                    client.execute(() -> {
//                        if (client.world != null) {
//                            Entity entity = client.world.getEntityById(fromTo[0]);
//                            if (entity instanceof ChainKnotEntity) {
//                                ((ChainKnotEntity) entity).removeHoldingEntityId(fromTo[1]);
//                            }
//                        }
//                    });
//                });
//
//        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_MULTI_CHAIN_ATTACH_PACKET_ID,
//                (client, handler, packetByteBuf, responseSender) -> {
//                    int from = packetByteBuf.readInt();
//                    int[] tos = packetByteBuf.readIntArray();
//                    client.execute(() -> {
//                        if (client.world != null) {
//                            Entity entity = client.world.getEntityById(from);
//                            if (entity instanceof ChainKnotEntity knot) {
//                                ((ChainKnotEntity) entity).addHoldingEntityIds(tos);
//                            }
//                        }
//                    });
//                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_CHAIN_ATTACH_PACKET_ID,
                (client, handler, packetByteBuf, responseSender) -> {
                    int fromId = packetByteBuf.readVarInt();
                    int toId = packetByteBuf.readVarInt();
                    int typeId = packetByteBuf.readVarInt();
                    client.execute(() -> {
                        if (client.world == null) return;
                        Entity from = client.world.getEntityById(fromId);
                        if (from instanceof ChainKnotEntity knot) {
                            Entity to = client.world.getEntityById(toId);
                            ChainType chainType = ClientInitializer.types.get(typeId);

                            if(to == null) {
                                INCOMPLETE_LINKS.add(new IncompleteChainLink(knot, toId, chainType));
                            } else {
                                ChainLink.create(knot, to, chainType);
                            }

//                            ChainLink link = new ChainLink(knot, to, chainType);
//                            knot.addLink(link);
//                            if (to instanceof ChainKnotEntity otherKnot) otherKnot.addLink(link);
                        } else {
                            throw new IllegalStateException("Tried to attach from " + from + " (#" + fromId + ") which is not a chain knot");
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_CHAIN_DETACH_PACKET_ID,
                (client, handler, packetByteBuf, responseSender) -> {
                    int fromId = packetByteBuf.readVarInt();
                    int toId = packetByteBuf.readVarInt();
                    client.execute(() -> {
                        if (client.world == null) return;
                        Entity from = client.world.getEntityById(fromId);
                        Entity to = client.world.getEntityById(toId);
                        if (from instanceof ChainKnotEntity knot) {
                            if(to == null) {
                                for (IncompleteChainLink link : INCOMPLETE_LINKS) {
                                    if(link.primary == from && link.secondaryId == toId)
                                        link.destroy();
                                }
                            } else {
                                for (ChainLink link : knot.getLinks()) {
                                    if (link.secondary == to) {
                                        link.destroy(true);
                                    }
                                }
                            }
                        } else {
                            throw new IllegalStateException("Tried to detach from " + from + " (#" + fromId + ") which is not a chain knot");
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_MULTI_CHAIN_ATTACH_PACKET_ID,
                (client, handler, packetByteBuf, responseSender) -> {
                    int fromId = packetByteBuf.readInt();
                    int[] toIds = packetByteBuf.readIntArray();
                    int[] types = packetByteBuf.readIntArray();
                    client.execute(() -> {
                        if (client.world == null) return;
                        Entity from = client.world.getEntityById(fromId);
                        if (from instanceof ChainKnotEntity knot) {
                            for (int i = 0; i < toIds.length; i++) {
                                Entity to = client.world.getEntityById(toIds[i]);
                                ChainType chainType = ClientInitializer.types.get(types[i]);

                                if(to == null) {
                                    INCOMPLETE_LINKS.add(new IncompleteChainLink(knot, toIds[i], chainType));
                                } else {
                                    ChainLink.create(knot, to, chainType);
                                }
//                                ChainLink link = new ChainLink(knot, to, chainType);
//                                knot.addLink(link);
//                                if (to instanceof ChainKnotEntity otherKnot) otherKnot.addLink(link);
                            }
                        } else {
                            throw new IllegalStateException("Tried to multi-attach from " + from + " (#" + fromId + ") which is not a chain knot");
                        }
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_SPAWN_CHAIN_COLLISION_PACKET,
                (client, handler, buf, responseSender) -> {
                    int entityTypeID = buf.readVarInt();
                    EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeID);
                    UUID uuid = buf.readUuid();
                    int entityId = buf.readVarInt();
                    Vec3d pos = PacketBufUtil.readVec3d(buf);

//                    int fromId = buf.readVarInt();
//                    int toId = buf.readVarInt();
                    int typeId = buf.readVarInt();

                    client.execute(() -> {
                        if (client.world == null) {
                            throw new IllegalStateException("Tried to spawn entity in a null world!");
                        }
                        Entity e = entityType.create(client.world);
                        if (e == null) {
                            throw new IllegalStateException("Failed to create instance of entity " + entityTypeID);
                        }
                        e.setPosition(pos.x, pos.y, pos.z);
                        e.setId(entityId);
                        e.setUuid(uuid);
                        e.setVelocity(Vec3d.ZERO);
                        if (e instanceof ChainCollisionEntity collider) {
                            collider.setChainType(ClientInitializer.types.get(typeId));
////                            collider.setStartOwnerId(startId);
////                            collider.setEndOwnerId(endId);
//                            Entity from = client.world.getEntityById(fromId);
//                            Entity to = client.world.getEntityById(toId);
//
//                            if(from == null || to == null) {
//                                long packedId = fromId | (long) toId << 32;
//                                ObjectList<ChainCollisionEntity> collidersList = INCOMPLETE_COLLIDERS.get(packedId);
//                                if(collidersList == null) {
//                                    collidersList = new ObjectArrayList<>(16);
//                                    INCOMPLETE_COLLIDERS.put(packedId, collidersList);
//                                }
//                                collidersList.add(collider);
//                            } else {
//                                // Find the link
//                                setColliderLink(collider, from, to);
//                            }
                        }
                        client.world.addEntity(entityId, e);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_SPAWN_CHAIN_KNOT_PACKET,
                (client, handler, buf, responseSender) -> {
                    int entityTypeId = buf.readVarInt();
                    EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeId);
                    UUID uuid = buf.readUuid();
                    int entityId = buf.readVarInt();
                    Vec3d pos = PacketBufUtil.readVec3d(buf);

                    int typeId = buf.readVarInt();

                    client.execute(() -> {
                        if (client.world == null) {
                            throw new IllegalStateException("Tried to spawn entity in a null world!");
                        }
                        Entity e = entityType.create(client.world);
                        if (e == null) {
                            throw new IllegalStateException("Failed to create instance of entity " + entityTypeId);
                        }
                        e.setPosition(pos.x, pos.y, pos.z);
                        e.setId(entityId);
                        e.setUuid(uuid);
                        e.setVelocity(Vec3d.ZERO);
                        if (e instanceof ChainKnotEntity knot) {
                            knot.setChainType(ClientInitializer.types.get(typeId));
                            knot.setGraceTicks((byte) 0);
                        }
                        client.world.addEntity(entityId, e);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_KNOT_CHANGE_TYPE_PACKET, (client, handler, buf, responseSender) -> {
            int knotId = buf.readVarInt();
            int typeId = buf.readVarInt();

            client.execute(() -> {
                if(client.world == null) return;
                ChainKnotEntity knot = (ChainKnotEntity) client.world.getEntityById(knotId);
                ChainType chainType = ClientInitializer.types.get(typeId);
                knot.updateChainType(chainType);
            });
        });

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            // Load client config
            ConnectibleChains.runtimeConfig.copyFrom(ConnectibleChains.fileConfig);
            getChainKnotEntityRenderer().getChainRenderer().purge();
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_CONFIG_SYNC_PACKET,
                (client, handler, packetByteBuf, responseSender) -> {
                    // Apply server config
                    if (client.isInSingleplayer()) {
                        return;
                    }
                    try {
                        ConnectibleChains.LOGGER.info("Received {} config from server", ConnectibleChains.MODID);
                        ConnectibleChains.runtimeConfig.readPacket(packetByteBuf);
                    } catch (Exception e) {
                        ConnectibleChains.LOGGER.error("Could not deserialize config: ", e);
                    }
                    getChainKnotEntityRenderer().getChainRenderer().purge();
                });

        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (result instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof ChainKnotEntity knot) {
                    return new ItemStack(knot.getChainType().getItem());
                } else if (entity instanceof ChainCollisionEntity collision) {
                    return new ItemStack(collision.getChainType().getItem());
                }
            }
            return ItemStack.EMPTY;
        });

        ClientTickEvents.START_WORLD_TICK.register(world -> {
            INCOMPLETE_LINKS.removeIf(IncompleteChainLink::tryCompleteOrRemove);
//            INCOMPLETE_COLLIDERS.forEach((packedId, colliderList) -> {
//                int startId = (int) (long) packedId;
//                int endId = (int) (packedId >> 32);
//
//                Entity from = world.getEntityById(startId);
//                Entity to = world.getEntityById(endId);
//
//                if(from != null && to != null) {
//                    for (ChainCollisionEntity collider : colliderList) {
//                        setColliderLink(collider, from, to);
//                    }
//                    INCOMPLETE_COLLIDERS.remove((long) packedId);
//                }
//            });
        });
    }

    private void setColliderLink(ChainCollisionEntity collider, Entity from, Entity to) {
        ChainKnotEntity knot = (ChainKnotEntity) from;
        for (ChainLink link : knot.getLinks()) {
            if(link.primary == knot && link.secondary == to) {
                collider.setLink(link);
                break;
            }
        }
    }

    public ChainKnotEntityRenderer getChainKnotEntityRenderer() {
        return chainKnotEntityRenderer;
    }

    public static ClientInitializer getInstance() {
        return instance;
    }
}
