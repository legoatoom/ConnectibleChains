package com.github.legoatoom.connectiblechains.networking.packet;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;

public record KnotChangeS2CPacket(int chainableId, Item sourceItem) implements CustomPayload {
    public static final Id<KnotChangeS2CPacket> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_knot_change_type_packet_id"));
    public static final PacketCodec<RegistryByteBuf, KnotChangeS2CPacket> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeVarInt(value.chainableId);
                buf.writeVarInt(Item.getRawId(value.sourceItem));
            }, buf -> new KnotChangeS2CPacket(buf.readVarInt(), Item.byRawId(buf.readVarInt())));

    private static void logBadActionTarget(Entity target, int targetId) {
        ConnectibleChains.LOGGER.warn("Tried to {} {} (#{}) which is not {}", "change type of", target, targetId, "chain knot");
    }

    @Override
    public Id<KnotChangeS2CPacket> getId() {
        return PAYLOAD_ID;
    }

    public void apply(ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> {
            if (client.world == null) return;
            Entity entity = client.world.getEntityById(chainableId);
            if (entity instanceof Chainable chainable) {
                chainable.setSourceItem(sourceItem);
            } else {
                logBadActionTarget(entity, chainableId);
            }
        });
    }

    public Packet<ClientCommonPacketListener> asPacket() {
        return ServerPlayNetworking.createS2CPacket(this);
    }
}
