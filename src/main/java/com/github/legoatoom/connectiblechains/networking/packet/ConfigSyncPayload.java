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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;

public record ConfigSyncPayload(float chainHangAmount, int maxChainRange) implements FabricPacket {
    public static final PacketType<ConfigSyncPayload> TYPE = PacketType.create(Helper.identifier("s2c_config_sync_packet_id"), ConfigSyncPayload::new);

    public ConfigSyncPayload(PacketByteBuf buf) {
        this(buf.readFloat(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeFloat(chainHangAmount);
        buf.writeInt(maxChainRange);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayerEntity ignoredClientPlayerEntity, PacketSender ignoredPacketSender) {
        MinecraftClient client = MinecraftClient.getInstance();
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
