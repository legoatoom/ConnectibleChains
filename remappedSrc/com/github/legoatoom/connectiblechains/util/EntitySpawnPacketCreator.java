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

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.function.Function;

/**
 * <a href="https://fabricmc.net/wiki/tutorial:projectiles">This class is from a tutorial</a> Edited some things to make it more useful for me.
 */
public class EntitySpawnPacketCreator {
    public static Packet<?> create(Entity e, Identifier packetID, Function<PacketByteBuf, PacketByteBuf> extraData) {
        if (e.world.isClient)
            throw new IllegalStateException("SpawnPacketUtil.create called on the logical client!");
        PacketByteBuf byteBuf = new PacketByteBuf(Unpooled.buffer());
        byteBuf.writeVarInt(Registry.ENTITY_TYPE.getRawId(e.getType()));
        byteBuf.writeUuid(e.getUuid());
        byteBuf.writeVarInt(e.getEntityId());

        PacketBufUtil.writeVec3d(byteBuf, e.getPos());
        PacketBufUtil.writeAngle(byteBuf, e.pitch);
        PacketBufUtil.writeAngle(byteBuf, e.yaw);
        byteBuf = extraData.apply(byteBuf);
        return ServerPlayNetworking.createS2CPacket(packetID, byteBuf);
    }
}