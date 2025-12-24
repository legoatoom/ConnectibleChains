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

package com.github.legoatoom.connectiblechains.mixin.flashback.record;


import com.github.legoatoom.connectiblechains.entity.Chainable;
import com.github.legoatoom.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.moulberry.flashback.record.Recorder;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@IfModLoaded(value = "flashback")
@Mixin(Recorder.class)
public class RecorderMixin  {

    @Inject(method = "writeCustomSnapshot", at = @At(value = "HEAD"), remap = false)
    private void chainDataSnapshot(Consumer<Packet<? super ClientPlayPacketListener>> consumer, CallbackInfo ci) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        ClientWorld world = minecraft.world;

        if (world == null) return;

        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Chainable chainable)) continue;

            for (Chainable.ChainData chainData : chainable.getChainDataSet()) {
                consumer.accept(new ChainAttachS2CPacket(entity, null, chainData.getChainHolder(), chainData.sourceItem).asPacket());
            }
        }
    }
}
