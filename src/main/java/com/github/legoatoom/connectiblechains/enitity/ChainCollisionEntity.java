/*
 *     Copyright (C) 2020 legoatoom
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.enitity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.List;

public class ChainCollisionEntity extends Entity {

    private float PushPower = .4f;

    private ChainKnotEntity owner;

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world){
        super(entityType, world);
        this.inanimate = true;
        this.teleporting = true;
    }

    public ChainCollisionEntity(World world, double x, double y, double z, ChainKnotEntity owner){
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.owner = owner;
        this.updatePosition(x, y, z);
        this.setBoundingBox(new Box(x,y,z,x,y,z).expand(.01d,0,.01d));
    }
    @Override
    public Box getBoundingBox() {
        return super.getBoundingBox();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return owner.damage(source, amount);
    }

    @Override
    public boolean collides() {
        return !this.removed;
    }

    @Override
    protected void destroy() {
        super.destroy();
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
    public void pushAwayFrom(Entity entity) {
        if (!this.isConnectedThroughVehicle(entity) && !isBelowMe(entity)) {
            if (!entity.noClip && !this.noClip) {
                double pushForceX = entity.getX() - this.getX();
                double pushForceY = entity.getY() - this.getY();
                double pushForceZ = entity.getZ() - this.getZ();
                double f = MathHelper.absMax(pushForceY, MathHelper.absMax(pushForceX, pushForceZ));
                if (f >= 0.009999999776482582D) {
                    f = (double)MathHelper.sqrt(f);
                    pushForceX /= f;
                    pushForceY /= f;
                    pushForceZ /= f;
                    double g = 1.0D / f;
                    if (g > 1.0D) {
                        g = 1.0D;
                    }

                    pushForceX *= g;
                    pushForceZ *= g;
                    pushForceY *= g;
                    pushForceY *= 0.05000000074505806D;
                    pushForceX *= 0.05000000074505806D;
                    pushForceZ *= 0.05000000074505806D;
                    pushForceY *= (double)(1.0F - this.pushSpeedReduction + PushPower);
                    pushForceX *= (double)(1.0F - this.pushSpeedReduction + PushPower);
                    pushForceZ *= (double)(1.0F - this.pushSpeedReduction + PushPower);

                    if (!entity.hasPassengers()) {
                        entity.addVelocity(pushForceX, pushForceY, pushForceZ);
                    }
                }
            }
        }
    }

    private boolean isBelowMe(Entity entity) {
        return this.getY() < entity.getY();
    }

    @Override
    protected boolean canClimb() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickCramming();
    }

    public void tickCramming() {
        List<Entity> list = this.world.getOtherEntities(this, this.getBoundingBox().expand(.05, 0, .05), EntityPredicates.canBePushedBy(this));
          if (!list.isEmpty()) {
              int i = this.world.getGameRules().getInt(GameRules.MAX_ENTITY_CRAMMING);
              int j;
              if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
                  j = 0;

                  for (int k = 0; k < list.size(); ++k) {
                      if (!((Entity) list.get(k)).hasVehicle()) {
                          ++j;
                      }
                  }
              }

              for (j = 0; j < list.size(); ++j) {
                  Entity entity = (Entity) list.get(j);
                  if (entity instanceof ChainCollisionEntity){continue;}
                  this.pushAwayFrom(entity);
              }
          }
    }

    @Override
    protected void initDataTracker() { }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.15F;
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new ChainCollisionEntitySpawnS2CPacket(this, owner.getEntityId());
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag tag) { }

    @Override
    protected void readCustomDataFromTag(CompoundTag tag) { }

    public static class ChainCollisionEntitySpawnS2CPacket extends EntitySpawnS2CPacket{
        private int ownerID;

        public int getOwnerID() {
            return ownerID;
        }

        public ChainCollisionEntitySpawnS2CPacket(ChainCollisionEntity chainCollisionEntity, int entityId) {
            super(chainCollisionEntity);
            this.ownerID = entityId;
        }

        @Override
        public void read(PacketByteBuf buf) throws IOException {
            super.read(buf);
            this.ownerID = buf.readInt();
        }

        @Override
        public void write(PacketByteBuf buf) throws IOException {
            super.write(buf);
            buf.writeInt(this.ownerID);
        }
    }
}
