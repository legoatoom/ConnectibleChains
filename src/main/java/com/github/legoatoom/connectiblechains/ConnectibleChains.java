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

package com.github.legoatoom.connectiblechains;


import com.github.legoatoom.connectiblechains.config.ModConfig;
import com.github.legoatoom.connectiblechains.entity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.item.ChainItemInfo;
import com.github.legoatoom.connectiblechains.networking.packet.Payloads;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mod Initializer for Connectible chains.
 */
public class ConnectibleChains implements ModInitializer {

    /**
     * All mods need to have an ID, that is what tells the game and fabric what each mod is.
     * These need to be unique for all mods, and always stay the same in your mod, so by creating a field
     * it will be a lot easier!
     */
    public static final String MODID = "connectiblechains";
    public static final Logger LOGGER = LogManager.getLogger("ConnectibleChains");
    /**
     * ModConfigs are helpful if people keep demanding for your chains to get longer...
     * File config is what's saved on disk, runtimeConfig should be used in most cases
     */
    public static ModConfig fileConfig;
    /**
     * Runtime config is a mix of the client and server config and should not be saved to disk
     */
    public static ModConfig runtimeConfig;

    /**
     * Here is where the fun begins.
     */
    @Override
    public void onInitialize() {

        ModEntityTypes.init();
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        fileConfig = configHolder.getConfig();
        runtimeConfig = new ModConfig().copyFrom(fileConfig);

        // On Clicking with a Chain event.
        UseBlockCallback.EVENT.register(ChainItemInfo::chainUseEvent);

        // Need this event on dedicated and internal server because of 'open to lan'.
        ServerPlayConnectionEvents.INIT.register((handler, server) -> fileConfig.syncToClient(handler.getPlayer()));
        Payloads.init();

    }

}
