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

package com.github.legoatoom.connectiblechains.mixin.server.network;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(EntityTrackerEntry.class)
abstract class EntityTrackerEntryMixin {
    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "sendPackets", at = @At("TAIL"))
    private void sendPackages(Consumer<Packet<?>> sender, CallbackInfo ci) {
        if (this.entity instanceof ChainKnotEntity chainKnotEntity) {
            PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
            //Write our id and the id of the one we connect to.
            int[] ids = chainKnotEntity.getHoldingEntities().stream().mapToInt(Entity::getId).toArray();
            if (ids.length > 0) {
                passedData.writeInt(chainKnotEntity.getId());
                passedData.writeIntArray(ids);
                sender.accept(ServerPlayNetworking.createS2CPacket(NetworkingPackages.S2C_MULTI_CHAIN_ATTACH_PACKET_ID, passedData));
            }
        }
    }


}
