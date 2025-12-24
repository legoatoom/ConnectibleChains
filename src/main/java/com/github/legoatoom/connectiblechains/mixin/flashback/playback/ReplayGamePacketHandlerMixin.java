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

package com.github.legoatoom.connectiblechains.mixin.flashback.playback;


import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import com.github.legoatoom.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.moulberry.flashback.playback.ReplayGamePacketHandler;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@IfModLoaded(value = "flashback")
@Mixin(ReplayGamePacketHandler.class)
public abstract class ReplayGamePacketHandlerMixin {

    @Shadow
    public abstract ServerWorld level();


    @Shadow
    protected abstract void forward(Packet<?> packet);

    /**
     * Loosely based on the method {@link ReplayGamePacketHandler#onEntityAttach(EntityAttachS2CPacket)}
     */
    @Inject(method = "onCustomPayload", at = @At(value = "HEAD"), cancellable = true)
    public void chainDataSnapshot(CustomPayloadS2CPacket clientboundCustomPayloadPacket, CallbackInfo ci) {
        if (clientboundCustomPayloadPacket.payload() instanceof ChainAttachS2CPacket(
                int attachedEntityId, int oldHoldingEntityId, int newHoldingEntityId, int chainTypeId
        )) {
            Entity source = this.level().getEntityById(attachedEntityId);
            if (source == null) {
                this.forward(clientboundCustomPayloadPacket);
                ci.cancel();
            } else {
                if (source instanceof ChainKnotEntity chainable) {
                    Entity previousHolder = null;
                    Entity newHolder = null;
                    Item sourceItem = Registries.ITEM.get(chainTypeId);
                    if (oldHoldingEntityId != 0) {
                        previousHolder = this.level().getEntityById(oldHoldingEntityId);
                        if (previousHolder == null) {
                            this.forward(clientboundCustomPayloadPacket);
                            ci.cancel();
                        }
                    }
                    if (newHoldingEntityId != 0) {
                        newHolder = this.level().getEntityById(newHoldingEntityId);
                        if (newHolder == null) {
                            this.forward(clientboundCustomPayloadPacket);
                            ci.cancel();
                        }
                    }
                    if (newHolder == null) {
                        // Detach
                        chainable.detachChain(new Chainable.ChainData(previousHolder, sourceItem));
                    } else {
                        // Attach/Replace
                        chainable.attachChain(new Chainable.ChainData(newHolder, sourceItem), previousHolder, true);
                    }

                    ci.cancel();
                }
            }
        }
    }
}
