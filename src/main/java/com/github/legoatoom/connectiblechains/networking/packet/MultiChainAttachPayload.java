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
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

@Deprecated
public record MultiChainAttachPayload(List<ChainAttachS2CPacket> packets) implements CustomPayload {
    public static final CustomPayload.Id<MultiChainAttachPayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_multi_chain_attach_packet_id"));
    public static final PacketCodec<RegistryByteBuf, MultiChainAttachPayload> PACKET_CODEC = PacketCodec.of(MultiChainAttachPayload::encode, MultiChainAttachPayload::decode);

    private static MultiChainAttachPayload decode(RegistryByteBuf buf) {
        return new MultiChainAttachPayload(buf.readList(ChainAttachS2CPacket::new));
    }

    private static void encode(MultiChainAttachPayload packet, RegistryByteBuf buf) {
        buf.writeCollection(packet.packets, (ChainAttachS2CPacket::encode));
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayNetworking.Context context) {
        this.packets.forEach(packet -> packet.apply(context));
    }

    @Override
    public Id<MultiChainAttachPayload> getId() {
        return PAYLOAD_ID;
    }
}
