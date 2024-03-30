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
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;

import java.util.List;

public record MultiChainAttachPacket(List<ChainAttachPacket> packets) implements FabricPacket {
    public static final PacketType<MultiChainAttachPacket> TYPE = PacketType.create(
            Helper.identifier("s2c_multi_chain_attach_packet_id"), MultiChainAttachPacket::new
    );


    public MultiChainAttachPacket(PacketByteBuf buf) {
        this(buf.readList(ChainAttachPacket::new));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeCollection(packets, (buf1, chainAttachPacket) -> chainAttachPacket.write(buf1));
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
        for (ChainAttachPacket packet : this.packets) {
            packet.apply(clientPlayerEntity, packetSender);
        }
    }
}
