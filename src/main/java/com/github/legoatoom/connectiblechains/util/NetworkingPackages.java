/*
 * Copyright (C) 2021 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.util;

import net.minecraft.util.Identifier;

public class NetworkingPackages {

    // Id for sending chain connection updates over the network.
    public static final Identifier S2C_CHAIN_ATTACH_PACKET_ID = Helper.identifier("s2c_chain_attach_packet_id");
    public static final Identifier S2C_CHAIN_DETACH_PACKET_ID = Helper.identifier("s2c_chain_detach_packet_id");
    public static final Identifier S2C_MULTI_CHAIN_ATTACH_PACKET_ID = Helper.identifier("s2c_multi_chain_attach_packet_id");
    public static final Identifier S2C_SPAWN_PACKET = Helper.identifier("s2c_spawn_packet_id");
}
