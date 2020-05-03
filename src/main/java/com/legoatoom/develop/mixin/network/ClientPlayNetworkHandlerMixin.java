package com.legoatoom.develop.mixin.network;

import com.legoatoom.develop.ConnectibleChains;
import com.legoatoom.develop.enitity.ChainKnotEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientWorld world;

    @Inject(
            method = "onEntitySpawn(Lnet/minecraft/network/packet/s2c/play/EntitySpawnS2CPacket;)V",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/network/packet/s2c/play/EntitySpawnS2CPacket;getEntityTypeId()Lnet/minecraft/entity/EntityType;"),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    ) // thank you parzivail
    private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci, double x, double y, double z, EntityType<?> type) {
        Entity entity = null;
        if (type == ConnectibleChains.CHAIN_KNOT) {
            entity = new ChainKnotEntity(this.world, new BlockPos(x, y, z));
        } // we can replicate this one here for all our other entities
        // entity would be null here when the type was not one for us
        if (entity != null) {
            int entityId = packet.getId();
            entity.setVelocity(Vec3d.ZERO); // entities always spawn standing still. We may change this later
            entity.updatePosition(x, y, z);
            entity.updateTrackedPosition(x, y, z);
            entity.pitch = (float) (packet.getPitch() * 360) / 256.0F;
            entity.yaw = (float) (packet.getYaw() * 360) / 256.0F;
            entity.setEntityId(entityId);
            entity.setUuid(packet.getUuid());
            this.world.addEntity(entityId, entity);
            ci.cancel(); // cancel stops the rest of the method to run (so no spawning code from mc runs)
        }
    }

    @Inject(
            method = "onEntityAttach(Lnet/minecraft/network/packet/s2c/play/EntityAttachS2CPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onEntityAttach(EntityAttachS2CPacket packet, CallbackInfo ci) {
        Entity entity = this.world.getEntityById(packet.getAttachedEntityId());
        if (entity instanceof ChainKnotEntity){
            ((ChainKnotEntity) entity).setHoldingEntity(packet.getHoldingEntityId());
            ci.cancel();
        }
    }

}
