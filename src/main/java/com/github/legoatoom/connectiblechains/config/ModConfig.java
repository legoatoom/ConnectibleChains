/*
 * Copyright (C) 2025 legoatoom
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.config;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.networking.packet.ConfigSyncPayload;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

@Config(name = ConnectibleChains.MODID)
public class ModConfig implements ConfigData {
    @SuppressWarnings("UnnecessaryModifier")
    @ConfigEntry.Gui.Excluded
    private static final transient boolean IS_DEBUG_ENV = FabricLoader.getInstance().isDevelopmentEnvironment();

    @ConfigEntry.Gui.Tooltip(count = 3)
    private float chainHangAmount = 8.0F;
    @ConfigEntry.BoundedDiscrete(max = 32)
    @ConfigEntry.Gui.Tooltip(count = 2)
    private int maxChainRange = 16;
    @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
    @ConfigEntry.Gui.Tooltip()
    private int quality = 4;

    @ConfigEntry.Gui.Tooltip()
    private boolean showToolTip = true;
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
        return IS_DEBUG_ENV && MinecraftClient.getInstance().debugHudEntryList.isF3Enabled();
    }

    public void syncToClients(MinecraftServer server) {
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            syncToClient(player);
        }
    }

    public void syncToClient(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new ConfigSyncPayload(chainHangAmount, maxChainRange));
    }

    public ModConfig copyFrom(ModConfig config) {
        this.chainHangAmount = config.chainHangAmount;
        this.maxChainRange = config.maxChainRange;
        this.quality = config.quality;
        this.showToolTip = config.showToolTip;
        return this;
    }

    public boolean doShowToolTip() {
        return showToolTip;
    }

}
