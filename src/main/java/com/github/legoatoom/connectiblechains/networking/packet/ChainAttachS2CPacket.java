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

import com.github.legoatoom.connectiblechains.entity.Chainable;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;


public record ChainAttachS2CPacket(int attachedEntityId, int oldHoldingEntityId, int newHoldingEntityId,
                                   int chainTypeId) implements FabricPacket {
    public static final PacketType<ChainAttachS2CPacket> TYPE = PacketType.create(Helper.identifier("s2c_chain_attach_packet_id"), ChainAttachS2CPacket::new);

    public ChainAttachS2CPacket(Entity attachedEntity, @Nullable Entity oldHoldingEntity, @Nullable Entity newHoldingEntity, Item souceItem) {
        this(attachedEntity.getId(), oldHoldingEntity != null ? oldHoldingEntity.getId() : 0, newHoldingEntity != null ? newHoldingEntity.getId() : 0, Registries.ITEM.getRawId(souceItem));
    }

    public ChainAttachS2CPacket(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf1) {
        buf1.writeInt(attachedEntityId);
        buf1.writeInt(oldHoldingEntityId);
        buf1.writeInt(newHoldingEntityId);
        buf1.writeInt(chainTypeId);
    }

    @Override
    public PacketType<ChainAttachS2CPacket> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayerEntity clientPlayerEntity, PacketSender ignoredPacketSender) {
        if (clientPlayerEntity.getWorld().getEntityById(this.attachedEntityId()) instanceof Chainable chainable) {
            chainable.addUnresolvedChainHolderId(this.oldHoldingEntityId(), this.newHoldingEntityId(), Registries.ITEM.get(this.chainTypeId()));
        }
    }

    public Packet<ClientPlayPacketListener> asPacket() {
        PacketByteBuf buf = PacketByteBufs.create();
        write(buf);
        return ServerPlayNetworking.createS2CPacket(TYPE.getId(), buf);
    }
}
