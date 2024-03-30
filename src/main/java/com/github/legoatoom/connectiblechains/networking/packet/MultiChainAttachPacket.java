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
