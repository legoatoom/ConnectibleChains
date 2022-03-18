/*
 * Copyright (C) 2022 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.config;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import io.netty.buffer.Unpooled;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

@Config(name = ConnectibleChains.MODID)
public class ModConfig implements ConfigData {
    @ConfigEntry.Gui.Excluded
    private static final transient boolean IS_DEBUG_ENV = FabricLoader.getInstance().isDevelopmentEnvironment();

    @ConfigEntry.Gui.Tooltip(count = 3)
    private float chainHangAmount = 9.0F;
    @ConfigEntry.BoundedDiscrete(max = 32)
    @ConfigEntry.Gui.Tooltip(count = 3)
    private int maxChainRange = 7;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
    @ConfigEntry.Gui.Tooltip()
    private int quality = 4;

    public float getChainHangAmount() {
        return chainHangAmount;
    }

    @SuppressWarnings("unused")
    public void setChainHangAmount(float chainHangAmount) {
        this.chainHangAmount = chainHangAmount;
    }

    public int getMaxChainRange() {
        return maxChainRange;
    }

    @SuppressWarnings("unused")
    public void setMaxChainRange(int maxChainRange) {
        this.maxChainRange = maxChainRange;
    }

    public int getQuality() {
        return quality;
    }

    @SuppressWarnings("unused")
    public void setQuality(int quality) {
        this.quality = quality;
    }

    public boolean doDebugDraw() {
        return IS_DEBUG_ENV && MinecraftClient.getInstance().options.debugEnabled;
    }

    public void syncToClients(MinecraftServer server) {
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            syncToClient(player);
        }
    }

    public void syncToClient(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, NetworkingPackets.S2C_CONFIG_SYNC_PACKET, this.writePacket());
    }

    public PacketByteBuf writePacket() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeFloat(chainHangAmount);
        return buf;
    }

    public void readPacket(PacketByteBuf buf) {
        this.chainHangAmount = buf.readFloat();
    }

    public ModConfig copyFrom(ModConfig config) {
        this.chainHangAmount = config.chainHangAmount;
        this.maxChainRange = config.maxChainRange;
        this.quality = config.quality;
        return this;
    }
}
