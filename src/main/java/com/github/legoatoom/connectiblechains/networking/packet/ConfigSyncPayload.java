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
import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record ConfigSyncPayload(float chainHangAmount, int maxChainRange) implements CustomPayload {
    public static final Id<ConfigSyncPayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_config_sync_packet_id"));
    public static final PacketCodec<PacketByteBuf, ConfigSyncPayload> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, ConfigSyncPayload::chainHangAmount,
                    PacketCodecs.INTEGER, ConfigSyncPayload::maxChainRange,
                    ConfigSyncPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }

    public void apply(ClientPlayNetworking.Context context) {
        MinecraftClient client = context.client();
        if (client.isInSingleplayer()) {
            return;
        }
        try {
            ConnectibleChains.LOGGER.info("Received {} config from server", ConnectibleChains.MODID);
            ConnectibleChains.runtimeConfig.setChainHangAmount(this.chainHangAmount);
            ConnectibleChains.runtimeConfig.setMaxChainRange(this.maxChainRange);
        } catch (Exception e) {
            ConnectibleChains.LOGGER.error("Could not deserialize config: ", e);
        }
        ClientInitializer.getInstance().getChainKnotEntityRenderer().ifPresent(r -> r.getChainRenderer().purge());
    }
}
