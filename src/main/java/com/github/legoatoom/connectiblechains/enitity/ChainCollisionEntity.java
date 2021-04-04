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

package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.util.EntitySpawnPacketCreator;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.function.Function;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for connections between chains.
 *
 * @author legoatoom
 */
public class ChainCollisionEntity extends Entity {

    /**
     * The chainKnot entity id that has a connection to another chainKnot with id {@link #endOwnerId}.
     */
    private int startOwnerId;
    /**
     * The chainKnot entity id that has a connection from another chainKnot with id {@link #startOwnerId}.
     */
    private int endOwnerId;

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world) {
        super(entityType, world);
        this.teleporting = true;
    }

    public ChainCollisionEntity(World world, double x, double y, double z, int startOwnerId, int endOwnerId) {
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.startOwnerId = startOwnerId;
        this.endOwnerId = endOwnerId;
        this.setPos(x, y, z);
//        this.setBoundingBox(new Box(x, y, z, x, y, z).expand(.01d, 0, .01d));
    }

    @Override
    protected void initDataTracker() {
        // Required by Entity
    }

    /**
     * We don't want this entity to be able to climb.
     * @return false
     */
    @Override
    protected boolean canClimb() {
        return false;
    }

    /**
     * When this entity is attacked by a player with a item that has Tag: {@link FabricToolTags#SHEARS},
     * it calls the {@link ChainKnotEntity#damageLink(boolean, ChainKnotEntity)} method
     * to destroy the link between the {@link #startOwnerId} and {@link #endOwnerId}
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.world.isClient && !this.removed) {
            Entity startOwner = this.world.getEntityById(startOwnerId);
            Entity endOwner = this.world.getEntityById(endOwnerId);
            Entity sourceEntity = source.getAttacker();
            if (sourceEntity instanceof PlayerEntity
                    && startOwner instanceof ChainKnotEntity && endOwner instanceof ChainKnotEntity) {
                boolean isCreative = ((PlayerEntity) sourceEntity).isCreative();
                if (!((PlayerEntity) sourceEntity).getMainHandStack().isEmpty() && ((PlayerEntity) sourceEntity).getMainHandStack().getItem().isIn(FabricToolTags.SHEARS)) {
                    ((ChainKnotEntity) startOwner).damageLink(isCreative, (ChainKnotEntity) endOwner);
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * If this entity can even be collided with.
     * Different from {@link #isCollidable()} ()} as this tells if something can collide with this.
     *
     * @return true
     */
    @Override
    public boolean collides() {
        return !this.removed;
    }

    /**
     * We don't want to be able to push the collision box of the chain.
     * @return false
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * We only allow the collision box to be rendered if a player is holding a item that has tag {@link FabricToolTags#SHEARS}.
     * This might be helpful when using F3+B to see the boxes of the chain.
     *
     * @return boolean - should the collision box be rendered.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && !player.getMainHandStack().isEmpty() && player.getMainHandStack().getItem().isIn(FabricToolTags.SHEARS)){
            return super.shouldRender(distance);
        } else {
            return false;
        }
    }

    @Override
    protected void readCustomDataFromTag(CompoundTag tag) {
        // Required by Entity, but does nothing.
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag tag) {
        // Required by Entity, but does nothing.
    }

    /**
     * Makes sure that nothing can walk through it.
     */
    @Override
    public boolean isCollidable() {
        return true;
    }

    /**
     * What happens when this is attacked?
     * This method is called by {@link PlayerEntity#attack(Entity)} to allow an entity to choose what happens when
     * it is attacked. We don't want to play sounds when we attack it without shears, so that is why we override this.
     */
    @Override
    public boolean handleAttack(Entity attacker) {
        playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        if (attacker instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) attacker;
            return this.damage(DamageSource.player(playerEntity), 0.0F);
        } else {
            return false;
        }
    }

    /**
     * When this entity is created we need to send a packet to the client.
     * This method sends a packet that contains the entityID of both the start and
     * end chainKnot of this entity.
     *
     */
    @Override
    public Packet<?> createSpawnPacket() {
        //Write our id and the id of the one we connect to.
        Function<PacketByteBuf, PacketByteBuf> extraData = packetByteBuf -> {
            packetByteBuf.writeVarInt(startOwnerId);
            packetByteBuf.writeVarInt(endOwnerId);
            return packetByteBuf;
        };
        return EntitySpawnPacketCreator.create(this, NetworkingPackages.S2C_SPAWN_CHAIN_COLLISION_PACKET, extraData);
    }

    public void setStartOwnerId(int startOwnerId) {
        this.startOwnerId = startOwnerId;
    }

    public void setEndOwnerId(int endOwnerId) {
        this.endOwnerId = endOwnerId;
    }
}
