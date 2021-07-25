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

package com.github.legoatoom.connectiblechains.mixin.server.world;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin is used to keep track of the connections when the ChainKnot is loaded again.
 *
 * If we do not do this, the client does not know about connections that are loaded in new chunks.
 *
 * @author legoatoom
 */
@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin {

    @Shadow
    @Final
    private Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers;

    @Inject(
            method = "sendChunkDataPackets",
            at = @At(value = "TAIL")
    )
    private void sendAttachChainPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk, CallbackInfo ci) {
        ObjectIterator<ThreadedAnvilChunkStorage.EntityTracker> var6 = this.entityTrackers.values().iterator();
        List<ChainKnotEntity> list = Lists.newArrayList();

        while (var6.hasNext()) {
            ThreadedAnvilChunkStorage.EntityTracker entityTracker = var6.next();
            Entity entity = entityTracker.entity;
            if (entity != player && entity.chunkX == chunk.getPos().x && entity.chunkZ == chunk.getPos().z) {
                if (entity instanceof ChainKnotEntity && !((ChainKnotEntity) entity).getHoldingEntities().isEmpty()) {
                    list.add((ChainKnotEntity) entity);
                }
            }
        }

        if (!list.isEmpty()) {
            for (ChainKnotEntity chainKnotEntity : list) {
                PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
                //Write our id and the id of the one we connect to.
                int[] ids = chainKnotEntity.getHoldingEntities().stream().mapToInt(Entity::getEntityId).toArray();
                if (ids.length > 0) {
                    passedData.writeInt(chainKnotEntity.getEntityId());
                    passedData.writeIntArray(ids);
                    ServerPlayNetworking.send(player, NetworkingPackages.S2C_MULTI_CHAIN_ATTACH_PACKET_ID, passedData);
                }
            }
        }
    }
}
