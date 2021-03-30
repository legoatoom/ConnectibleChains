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

import com.github.legoatoom.connectiblechains.network.packet.s2c.play.ChainCollisionEntitySpawnS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class ChainCollisionEntity extends Entity {

    @SuppressWarnings("FieldCanBeLocal")
    private final float pushPower = .4f;

    private int startOwnerId;
    private int endOwnerId;

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world){
        super(entityType, world);
        this.teleporting = true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        return false;
    }

    public ChainCollisionEntity(World world, double x, double y, double z, int startOwnerId, int endOwnerId){
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.startOwnerId = startOwnerId;
        this.endOwnerId = endOwnerId;
        this.updatePosition(x, y, z);
        this.setBoundingBox(new Box(x,y,z,x,y,z).expand(.01d,0,.01d));
    }


    @Override
    public boolean damage(DamageSource source, float amount) {
        Entity entity = this.world.getEntityById(startOwnerId);
        if (entity != null && !this.removed){
            if (entity instanceof ChainKnotEntity && !entity.removed){
                ((ChainKnotEntity) entity).damageLink(endOwnerId);
                return true;
            }
            return entity.damage(source, amount);
        } else if (this.removed){
            return false;
        } else{
            this.remove();
            return true;
        }
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity)attacker;
            return this.damage(DamageSource.player(playerEntity), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public boolean collides() {
        return !this.removed;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        super.onPlayerCollision(player);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canClimb() {
        return false;
    }

    @Override
    protected void initDataTracker() { }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new ChainCollisionEntitySpawnS2CPacket(this, startOwnerId, endOwnerId);
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag tag) { }

    @Override
    protected void readCustomDataFromTag(CompoundTag tag) { }

}
