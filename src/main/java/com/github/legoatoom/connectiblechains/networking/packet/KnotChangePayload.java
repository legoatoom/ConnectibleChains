/*
 * Copyright (C) 2024 legoatoom
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

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record KnotChangePayload(int knotId, Item sourceItem) implements CustomPayload {
    public static final Id<KnotChangePayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_knot_change_type_packet_id"));
    public static final PacketCodec<RegistryByteBuf, KnotChangePayload> PACKET_CODEC =
            PacketCodec.of((value, buf) -> {
                buf.writeVarInt(value.knotId);
                buf.writeVarInt(Item.getRawId(value.sourceItem));
            }, buf -> new KnotChangePayload(buf.readVarInt(), Item.byRawId(buf.readVarInt())));


    @Override
    public Id<KnotChangePayload> getId() {
        return PAYLOAD_ID;
    }

    public void apply(ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        client.execute(() -> {
            if (client.world == null) return;
            Entity entity = client.world.getEntityById(knotId);
            if (entity instanceof ChainKnotEntity knot) {
                knot.updateChainType(sourceItem);
            } else {
                logBadActionTarget(entity, knotId);
            }
        });
    }

    private static void logBadActionTarget(Entity target, int targetId) {
        ConnectibleChains.LOGGER.warn(String.format("Tried to %s %s (#%d) which is not %s",
                "change type of", target, targetId, "chain knot"
        ));
    }
}
