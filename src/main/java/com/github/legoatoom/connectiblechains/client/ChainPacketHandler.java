package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.chain.IncompleteChainLink;
import com.github.legoatoom.connectiblechains.enitity.ChainCollisionEntity;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import com.github.legoatoom.connectiblechains.util.PacketBufUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.UUID;

public class ChainPacketHandler {
    /**
     * Links where this is the primary and the secondary doesn't yet exist / hasn't yet loaded.
     * They are kept in a separate list to prevent accidental accesses of the secondary which would
     * result in a NPE. The links will try to be completed each world tick.
     */
    private final ObjectList<IncompleteChainLink> incompleteLinks = new ObjectArrayList<>(256);

    public ChainPacketHandler() {
        register();
    }

    private void register() {
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
                            ChainType chainType = ChainTypesRegistry.REGISTRY.get(typeId);

                            if (to == null) {
                                incompleteLinks.add(new IncompleteChainLink(knot, toId, chainType));
                            } else {
                                ChainLink.create(knot, to, chainType);
                            }
                        } else {
                            throw createBadActionTargetException("attach from", from, fromId, "chain knot");
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
                            if (to == null) {
                                for (IncompleteChainLink link : incompleteLinks) {
                                    if (link.primary == from && link.secondaryId == toId)
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
                            throw createBadActionTargetException("detach from", from, fromId, "chain knot");
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
                                ChainType chainType = ChainTypesRegistry.REGISTRY.get(types[i]);

                                if (to == null) {
                                    incompleteLinks.add(new IncompleteChainLink(knot, toIds[i], chainType));
                                } else {
                                    ChainLink.create(knot, to, chainType);
                                }
                            }
                        } else {
                            throw createBadActionTargetException("multi-attach from", from, fromId, "chain knot");
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
                            collider.setChainType(ChainTypesRegistry.REGISTRY.get(typeId));
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
                            knot.setChainType(ChainTypesRegistry.REGISTRY.get(typeId));
                            knot.setGraceTicks((byte) 0);
                        }
                        client.world.addEntity(entityId, e);
                    });
                });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_KNOT_CHANGE_TYPE_PACKET, (client, handler, buf, responseSender) -> {
            int knotId = buf.readVarInt();
            int typeId = buf.readVarInt();

            client.execute(() -> {
                if (client.world == null) return;
                Entity entity = client.world.getEntityById(knotId);
                ChainType chainType = ChainTypesRegistry.REGISTRY.get(typeId);
                if (entity instanceof ChainKnotEntity knot) {
                    knot.updateChainType(chainType);
                } else {
                    throw createBadActionTargetException("change type of", entity, knotId, "chain knot");
                }
            });
        });
    }

    private RuntimeException createBadActionTargetException(String action, Entity target, int targetId, String expectedTarget) {
        return new IllegalStateException(String.format("Tried to %s %s (#%d) which is not %s",
                action, target, targetId, expectedTarget
        ));
    }

    public void tick() {
        incompleteLinks.removeIf(IncompleteChainLink::tryCompleteOrRemove);
    }
}
