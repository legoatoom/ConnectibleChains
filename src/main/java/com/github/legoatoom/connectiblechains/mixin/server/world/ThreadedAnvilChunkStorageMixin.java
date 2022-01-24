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

package com.github.legoatoom.connectiblechains.mixin.server.world;

import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import com.github.legoatoom.connectiblechains.util.PacketCreator;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.mutable.MutableObject;
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
    private void sendAttachChainPackets(ServerPlayerEntity player, MutableObject<ChunkDataS2CPacket> cachedDataPacket, WorldChunk chunk, CallbackInfo ci) {
        ObjectIterator<ThreadedAnvilChunkStorage.EntityTracker> trackers = this.entityTrackers.values().iterator();
        List<ChainKnotEntity> knots = Lists.newArrayList();

        while (trackers.hasNext()) {
            ThreadedAnvilChunkStorage.EntityTracker entityTracker = trackers.next();
            Entity entity = entityTracker.entity;
            if (entity != player && entity.getChunkPos().equals(chunk.getPos())) {
                if (entity instanceof ChainKnotEntity knot && !knot.getLinks().isEmpty()) {
                    knots.add(knot);
                }
            }
        }

        for (ChainKnotEntity knot : knots) {
            Packet<?> packet = PacketCreator.createMultiAttach(knot);
            if(packet != null) player.networkHandler.sendPacket(packet);
        }
    }
}
