package com.legoatoom.develop.enitity;

import com.legoatoom.develop.ConnectibleChains;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ChainKnotEntity extends AbstractDecorationEntity {

    private Entity holdingEntity;
    private int holdingEntityId;
    private CompoundTag chainTag;

    public ChainKnotEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ConnectibleChains.CHAIN_KNOT, world, pos);
        this.updatePosition((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
        float f = 0.125F;
        float g = 0.1875F;
        float h = 0.25F;
        this.setBoundingBox(new Box(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
        this.teleporting = true;
    }

    @Override
    public void updatePosition(double x, double y, double z) {
        super.updatePosition((double) MathHelper.floor(x) + 0.5D, (double)MathHelper.floor(y) + 0.5D, (double)MathHelper.floor(z) + 0.5D);
    }

    @Override
    protected void updateAttachmentPosition() {
        this.setPos((double)this.attachmentPos.getX() + 0.5D, (double)this.attachmentPos.getY() + 0.5D, (double)this.attachmentPos.getZ() + 0.5D);
    }

    @Override
    public void setFacing(Direction facing) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient) {
            this.updateChain();
        }
    }

    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);
        CompoundTag compoundTag3;
        if (this.holdingEntity != null) {
            compoundTag3 = new CompoundTag();
            if (this.holdingEntity instanceof LivingEntity) {
                UUID uUID = this.holdingEntity.getUuid();
                compoundTag3.putUuidNew("UUID", uUID);
            } else if (this.holdingEntity instanceof AbstractDecorationEntity) {
                BlockPos blockPos = ((AbstractDecorationEntity) this.holdingEntity).getDecorationBlockPos();
                compoundTag3.putInt("X", blockPos.getX());
                compoundTag3.putInt("Y", blockPos.getY());
                compoundTag3.putInt("Z", blockPos.getZ());
            }

            tag.put("Chain", this.chainTag.copy());
        } else if (this.chainTag != null) {
            tag.put("Chain", this.chainTag.copy());
        }
    }

    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        if (tag.contains("Chain", 10)){
            this.chainTag = tag.getCompound("Leash");
        }
    }

    /**
     * Called when a player interacts with this entity.
     *
     * @param player the player
     * @param hand   the hand the player used to interact with this entity
     */
    @Override
    public boolean interact(PlayerEntity player, Hand hand) {
        if (this.world.isClient){
            return true;
        } else {
            ItemStack itemStack = player.getStackInHand(hand);
            if (!this.isAlive()) {
                return false;
            } else {
                if (this.getHoldingEntity() == player) {
                    this.detachChain(true, !player.abilities.creativeMode);
                } else {
                    boolean bl = false;
                    double d = 7.0D;
                    List<ChainKnotEntity> list = this.world.getNonSpectatingEntities(ChainKnotEntity.class,
                            new Box(this.getX() - 7.0D, this.getY() - 7.0D, this.getZ() - 7.0D,
                                    this.getX() + 7.0D, this.getY() + 7.0D, this.getZ() + 7.0D));
                    Iterator<ChainKnotEntity> var7 = list.iterator();

                    ChainKnotEntity chainKnotEntity;
                    while(var7.hasNext()){
                        chainKnotEntity = (ChainKnotEntity)var7.next();
                        if (chainKnotEntity.getHoldingEntity() == player){
                            chainKnotEntity.attachChain(this, true);
                            bl = true;
                        }
                    }
                    if (!bl) {
                        if (itemStack.getItem() == ConnectibleChains.TEMP_CHAIN && this.canbeChainedBy(player)) {
                            this.attachChain(player, true);
                            itemStack.decrement(1);
                            return true;
                        } else {
                            this.remove();
                            if (player.abilities.creativeMode) {
                                var7 = list.iterator();
                                while (var7.hasNext()) {
                                    chainKnotEntity = var7.next();
                                    if (chainKnotEntity.isChained() && chainKnotEntity.getHoldingEntity() == this) {
                                        chainKnotEntity.detachChain(true, false);
                                    }
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }
    }

    public boolean canStayAttached() {
        return this.world.getBlockState(this.attachmentPos).getBlock().isIn(BlockTags.FENCES);
    }

    public static ChainKnotEntity getOrCreate(World world, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class,
                new Box((double)i - 1.0D, (double)j - 1.0D, (double)k - 1.0D,
                        (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D));
        Iterator<ChainKnotEntity> var6 = list.iterator();

        ChainKnotEntity chainKnotEntity;
        do {
            if (!var6.hasNext()) {
                ChainKnotEntity leashKnotEntity2 = new ChainKnotEntity(world, pos);
                world.spawnEntity(leashKnotEntity2);
                leashKnotEntity2.onPlace();
                return leashKnotEntity2;
            }

            chainKnotEntity = var6.next();
        } while(!chainKnotEntity.getDecorationBlockPos().equals(pos));

        return chainKnotEntity;
    }

    public void updateChain() {
        if (this.chainTag != null) {
            this.deserializeChainTag();
        }

        if (this.holdingEntity != null) {
            if (!this.isAlive() || !this.holdingEntity.isAlive()) {
                this.detachChain(true, true);
            }
        }
    }

    public void detachChain(boolean sendPacket, boolean bl){
        if (this.holdingEntity != null){
            this.teleporting = false;
            if (!(this.holdingEntity instanceof PlayerEntity)){
                this.holdingEntity.teleporting = false;
            }

            this.holdingEntity = null;
            this.chainTag = null;
            if (!this.world.isClient && bl){
                this.dropItem(ConnectibleChains.TEMP_CHAIN);
            }

            if (!this.world.isClient && sendPacket && this.world instanceof ServerWorld) {
                ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(
                        this, new EntityAttachS2CPacket(this, (Entity)null));
            }
        }
    }

    public boolean canbeChainedBy(PlayerEntity player) {
        return !this.isChained();
    }

    public boolean isChained() {
        return this.holdingEntity != null;
    }

    public Entity getHoldingEntity() {
        if (this.holdingEntity == null && this.holdingEntityId != 0 && this.world.isClient){
            this.holdingEntity = this.world.getEntityById(this.holdingEntityId);
        }

        return this.holdingEntity;
    }

    public void attachChain(Entity entity, boolean bl){
        this.holdingEntity = entity;
        this.chainTag = null;
        this.teleporting = true;
        if(!(this.holdingEntity instanceof PlayerEntity)){
            this.holdingEntity.teleporting = true;
        }

        if(!this.world.isClient && bl && this.world instanceof ServerWorld){
            ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(this,
                    new EntityAttachS2CPacket(this, this.holdingEntity));
        }
    }

    @Environment(EnvType.CLIENT)
    public void setHoldingEntity(int id){
        this.holdingEntityId = id;
        this.detachChain(false, false);
    }

    private void deserializeChainTag() {
        if (this.chainTag != null && this.world instanceof ServerWorld) {
            if (this.chainTag.containsUuidNew("UUID")){
                UUID uUID = this.chainTag.getUuidNew("UUID");
                Entity entity = ((ServerWorld)this.world).getEntity(uUID);
                if (entity != null){
                    this.attachChain(entity, true);
                }
            } else if (this.chainTag.contains("X", 99)
                    && this.chainTag.contains("Y", 99)
                    && this.chainTag.contains("Z", 99)){
                BlockPos blockPos = new BlockPos(this.chainTag.getInt("X"),
                                                    this.chainTag.getInt("Z"),
                                                        this.chainTag.getInt("Z"));
                this.attachChain(ChainKnotEntity.getOrCreate(this.world, blockPos), true);
            } else {
                this.detachChain(false, true);
            }

            if (this.age > 100) {
                this.chainTag = null;
            }
        }
    }

    @Override
    public int getWidthPixels() {
        return 9;
    }

    @Override
    public int getHeightPixels() {
        return 9;
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return -0.0625F;
    }



    @Environment(EnvType.CLIENT)
    public boolean shouldRender(double distance) {
        return distance < 1024.0D;
    }

    @Override
    public void onBreak(Entity entity) {
        this.playSound(SoundEvents.BLOCK_CHAIN_FALL, 1.0F, 1.0F);
    }

    @Override
    public void onPlace() {
        this.playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.getType(), 0, this.getDecorationBlockPos());
    }
}
