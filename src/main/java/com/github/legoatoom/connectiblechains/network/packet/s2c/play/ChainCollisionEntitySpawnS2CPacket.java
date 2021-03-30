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

package com.github.legoatoom.connectiblechains.network.packet.s2c.play;

import com.github.legoatoom.connectiblechains.enitity.ChainCollisionEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;

import java.io.IOException;

public class ChainCollisionEntitySpawnS2CPacket extends EntitySpawnS2CPacket {
    private int startOwnerID, endOwnerID;

    public int getStartOwnerID() {
        return startOwnerID;
    }

    public int getEndOwnerID() {
        return endOwnerID;
    }

    public ChainCollisionEntitySpawnS2CPacket(ChainCollisionEntity chainCollisionEntity, int startOwnerID, int endOwnerID) {
        super(chainCollisionEntity);
        this.startOwnerID = startOwnerID;
        this.endOwnerID = endOwnerID;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        super.read(buf);
        this.startOwnerID = buf.readInt();
        this.endOwnerID = buf.readInt();
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        super.write(buf);
        buf.writeInt(this.startOwnerID);
        buf.writeInt(this.endOwnerID);
    }
}
