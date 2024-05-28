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
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class Payloads {


    public static void init() {
        ConnectibleChains.LOGGER.info("Register Custom Payloads for Networking.");
        PayloadTypeRegistry.playS2C().register(ChainAttachPayload.PAYLOAD_ID, ChainAttachPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(MultiChainAttachPayload.PAYLOAD_ID, MultiChainAttachPayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(KnotChangePayload.PAYLOAD_ID, KnotChangePayload.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.PAYLOAD_ID, ConfigSyncPayload.PACKET_CODEC);
    }
}
