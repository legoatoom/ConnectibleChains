/*
 * Copyright (C) 2024 legoatoom.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.networking.packet;

import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;


public record ChainAttachS2CPacket(int attachedEntityId, int oldHoldingEntityId, int newHoldingEntityId,
                                   int chainTypeId) implements CustomPayload {
    public static final Id<ChainAttachS2CPacket> PAYLOAD_ID = new Id<>(Helper.identifier("s2c_chain_attach_packet_id"));
    public static final PacketCodec<RegistryByteBuf, ChainAttachS2CPacket> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, ChainAttachS2CPacket::attachedEntityId,
                    PacketCodecs.INTEGER, ChainAttachS2CPacket::oldHoldingEntityId,
                    PacketCodecs.INTEGER, ChainAttachS2CPacket::newHoldingEntityId,
                    PacketCodecs.INTEGER, ChainAttachS2CPacket::chainTypeId,
                    ChainAttachS2CPacket::new);

    public ChainAttachS2CPacket(Entity attachedEntity, @Nullable Entity oldHoldingEntity, @Nullable Entity newHoldingEntity, Item souceItem) {
        this(attachedEntity.getId(), oldHoldingEntity != null ? oldHoldingEntity.getId() : 0, newHoldingEntity != null ? newHoldingEntity.getId() : 0, Registries.ITEM.getRawId(souceItem));
    }

    public ChainAttachS2CPacket(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void encode(PacketByteBuf buf1, ChainAttachS2CPacket packet) {
        buf1.writeInt(packet.attachedEntityId);
        buf1.writeInt(packet.oldHoldingEntityId);
        buf1.writeInt(packet.newHoldingEntityId);
        buf1.writeInt(packet.chainTypeId);
    }


    private void applyDetach(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
//        ClientWorld world = clientPlayerEntity.clientWorld;
//        Entity primary = world.getEntityById(primaryEntityId);
//
//        if (!(primary instanceof ChainKnotEntity primaryKnot)) {
//            LOGGER.warn(String.format("Tried to detach from %s (#%d) which is not a chain knot",
//                    primary, primaryEntityId
//            ));
//            return;
//        }
//        Entity secondary = world.getEntityById(secondaryEntityId);
//        incompleteLinks.removeIf(link -> {
//            if (link.primary == primaryKnot && link.secondaryId == secondaryEntityId) {
//                link.destroy();
//                return true;
//            }
//            return false;
//        });
//
//        if (secondary == null) {
//            return;
//        }
//
//        for (ChainLink link : primaryKnot.getLinks()) {
//            if (link.getSecondary() == secondary) {
//                link.destroy(true);
//            }
//        }
    }

    private void applyAttach(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
//        ClientWorld world = clientPlayerEntity.clientWorld;
//        Entity primary = world.getEntityById(attachedEntityId);
//
//        if (!(primary instanceof ChainKnotEntity primaryKnot)) {
//            LOGGER.warn(String.format("Tried to attach from %s (#%d) which is not a chain knot",
//                    primary, attachedEntityId
//            ));
//            return;
//        }
//        Entity secondary = world.getEntityById(holdingEntityId);
//
//        Item chainType = Registries.ITEM.get(chainTypeId);
//
//        if (secondary == null) {
//            incompleteLinks.add(new IncompleteChainLink(primaryKnot, holdingEntityId, chainType));
//        } else {
//            ChainLink.create(primaryKnot, secondary, chainType);
//        }
    }

    @Override
    public Id<ChainAttachS2CPacket> getId() {
        return PAYLOAD_ID;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayNetworking.Context context) {
        if (attach){
            applyAttach(context.player(), context.responseSender());
        } else {
            applyDetach(context.player(), context.responseSender());
        }
    }

    public Packet<ClientCommonPacketListener> asPacket() {
        return ServerPlayNetworking.createS2CPacket(this);
    }
}
