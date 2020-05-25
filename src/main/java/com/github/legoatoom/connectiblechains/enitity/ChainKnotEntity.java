package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.network.packet.s2c.play.EntitiesAttachS2CPacket;
import com.sun.net.httpserver.Filter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Packet;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChainKnotEntity extends AbstractDecorationEntity {

    private List<Entity> holdingEntities  = new ArrayList<>();
    private final List<Integer> holdingEntitiesId  = new ArrayList<>();

    public List<Integer> holdedByEntitiesId = new ArrayList<>();
    private ListTag chainTag;

    public ChainKnotEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ConnectibleChains.CHAIN_KNOT, world, pos);
        this.updatePosition((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
        float f = 0.125F;
        float g = 0.1875F;
        float h = 0.25F;
        this.setBoundingBox(new Box(this.getX() - g, this.getY() - h + f, this.getZ() - g, this.getX() + g, this.getY() + h + f, this.getZ() + g));
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
        if (!this.holdingEntities.isEmpty()) {
            ListTag list = new ListTag();
            for (Entity entity : holdingEntities){
                CompoundTag compoundTag = new CompoundTag();
                if (entity instanceof LivingEntity) {
                    UUID uUID = entity.getUuid();
                    compoundTag.putUuidNew("UUID", uUID);
                } else if (entity instanceof AbstractDecorationEntity) {
                    BlockPos blockPos = ((AbstractDecorationEntity)entity).getDecorationBlockPos();
                    compoundTag.putInt("X", blockPos.getX());
                    compoundTag.putInt("Y", blockPos.getY());
                    compoundTag.putInt("Z", blockPos.getZ());
                }
                list.add(compoundTag);
            }
            tag.put("Chain", list);


        } else if (chainTag != null) {
            tag.put("Chain", this.chainTag.copy());
        }
        Box box = this.getBoundingBox();
        CompoundTag boxTag = new CompoundTag();
        boxTag.putDouble("maxX",box.maxX);
        boxTag.putDouble("maxY",box.maxY);
        boxTag.putDouble("maxZ",box.maxZ);
        boxTag.putDouble("minX",box.minX);
        boxTag.putDouble("minY",box.minY);
        boxTag.putDouble("minZ",box.minZ);
        tag.put("BoundBox", boxTag);
        super.writeCustomDataToTag(tag);
    }
//
    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);

        if (tag.contains("BoundBox")){
            CompoundTag boxTag = tag.getCompound("BoundBox");
            double maxX = boxTag.getDouble("maxX");
            double maxY = boxTag.getDouble("maxY");
            double maxZ = boxTag.getDouble("maxZ");
            double minX = boxTag.getDouble("minX");
            double minY = boxTag.getDouble("minY");
            double minZ = boxTag.getDouble("minZ");
            Box box = new Box(maxX,maxY,maxZ,minX,minY,minZ);
            this.setBoundingBox(box);
        }
        if (tag.contains("Chain")) {
            this.chainTag = (ListTag) tag.get("Chain");
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
                if (this.getHoldingEntities().contains(player)) {
                    this.detachChain(true, false, player);
                    if (!player.isCreative()) {
                        player.giveItemStack(new ItemStack(ConnectibleChains.TEMP_CHAIN, 1));
                    }
                } else {
                    boolean bl = false;
                    double d = 7.0D;
                    List<ChainKnotEntity> list = this.world.getNonSpectatingEntities(ChainKnotEntity.class,
                            new Box(this.getX() - d, this.getY() - d, this.getZ() - d,
                                    this.getX() + d, this.getY() + d, this.getZ() + d));
                    Iterator<ChainKnotEntity> var7 = list.iterator();

                    ChainKnotEntity chainKnotEntity;
                    while(var7.hasNext()){
                        chainKnotEntity = var7.next();
                        if (chainKnotEntity.getHoldingEntities().contains(player)
                                && !this.holdedByEntitiesId.contains(chainKnotEntity.getEntityId())
                                && !this.getHoldingEntities().contains(chainKnotEntity)){
                            chainKnotEntity.attachChain(this, true, player.getEntityId());
                            chainKnotEntity.detachChain(false, false, player);
                            bl = true;
                        }
                    }
                    if (!bl) {
                        if (itemStack.getItem() == ConnectibleChains.TEMP_CHAIN) {
                            this.attachChain(player, true, 0);
                            itemStack.decrement(1);
                            return true;
                        } else {
                            this.remove();
                            if (player.abilities.creativeMode) {
                                var7 = list.iterator();
                                while (var7.hasNext()) {
                                    chainKnotEntity = var7.next();
                                    chainKnotEntity.detachChain(true, false, player);
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
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box(pos).expand(1.0D));
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

        List<Entity> copy = new ArrayList<>(this.holdingEntities);
        for (Entity entity1 : copy){
            if (!this.isAlive() || !entity1.isAlive() || !entity1.isInRange(this, 7.0D)){
                this.detachChain(true, false, entity1);
                entity1.dropItem(ConnectibleChains.TEMP_CHAIN);
            }
        } }

    public void detachChain(boolean sendPacket, boolean bl, Entity entity){
        if (this.getHoldingEntities().contains(entity) || !entity.isAlive()){
            this.teleporting = false;
            if (!(entity instanceof PlayerEntity)){
                entity.teleporting = false;
                ((ChainKnotEntity) entity).holdedByEntitiesId.remove((Object) this.getEntityId());
            }
            this.holdingEntities.remove(entity);
            this.holdingEntitiesId.remove((Object) entity.getEntityId());

            if (!this.world.isClient && bl){
                this.dropItem(ConnectibleChains.TEMP_CHAIN);
            }

            if (!this.world.isClient && sendPacket && this.world instanceof ServerWorld) {
                ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(
                        this, new EntitiesAttachS2CPacket(this, entity.getEntityId(), true, 0));
            }
        }
    }

    public List<Entity> getHoldingEntities() {
        if (this.world.isClient && this.holdingEntitiesId.size() != this.holdingEntities.size()){
            this.holdingEntities = this.holdingEntitiesId.stream().map(integer -> this.world.getEntityById(integer)).collect(Collectors.toList());
        }
        return this.holdingEntities;
    }

    public void attachChain(Entity entity, boolean bl, int playerFrom){
        if (!this.getHoldingEntities().contains(entity)){
            this.holdingEntities.add(entity);
            this.holdingEntitiesId.add(entity.getEntityId());
            this.teleporting = true;
            if(!(entity instanceof PlayerEntity)){
                entity.teleporting = true;
                ((ChainKnotEntity) entity).holdedByEntitiesId.add(this.getEntityId());
            }
            if(!this.world.isClient && bl && this.world instanceof ServerWorld){
                ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(this,
                        new EntitiesAttachS2CPacket(this, entity.getEntityId(), false, playerFrom));
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public void addHoldingEntity(int id, int fromId){
        Entity entity = this.world.getEntityById(id);
        if (entity instanceof ChainKnotEntity){
            ((ChainKnotEntity) entity).holdedByEntitiesId.add(this.getEntityId());
        }
        if (fromId != 0){
            Entity from = this.world.getEntityById(fromId);
            this.detachChain(false, false, from);
        }
        if (!this.holdingEntitiesId.contains(id)){
            this.holdingEntitiesId.add(id);
        }
    }

    @Environment(EnvType.CLIENT)
    public void removeHoldingEntity(int id){
        if (this.holdingEntitiesId.contains(id)){
            this.holdingEntitiesId.remove((Object) id);
        }
        Entity entity = this.world.getEntityById(id);
        if (entity instanceof ChainKnotEntity){
            ((ChainKnotEntity) entity).holdedByEntitiesId.remove((Object) this.getEntityId());
        }
    }

    private void deserializeChainTag() {
        if (this.chainTag != null && this.world instanceof ServerWorld && this.age > 10) {
            for (Tag tag : chainTag){
                if (tag instanceof CompoundTag) {
                    if (((CompoundTag) tag).containsUuidNew("UUID")) {
                        UUID uUID = ((CompoundTag) tag).getUuidNew("UUID");
                        Entity entity = ((ServerWorld)this.world).getEntity(uUID);
                        if (entity != null) {
                            this.attachChain(entity, true, 0);
                        }
                    } else if (((CompoundTag) tag).contains("X", 99 ) && ((CompoundTag) tag).contains("Y", 99) && ((CompoundTag) tag).contains("Z", 99)) {
                        BlockPos blockPos = new BlockPos(((CompoundTag) tag).getInt("X"), ((CompoundTag) tag).getInt("Y"), ((CompoundTag) tag).getInt("Z"));
                        ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.world, blockPos);
                        this.holdingEntities.add(entity);
                        this.holdingEntitiesId.add(entity.getEntityId());
                        this.teleporting = true;
                            ((ServerWorld)this.world).getChunkManager().sendToOtherNearbyPlayers(this,
                                    new EntitiesAttachS2CPacket(this, entity.getEntityId(), false, 0));
                    }
                }
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
        for (int id : new ArrayList<>(this.holdedByEntitiesId)){
            Entity entity1 = this.world.getEntityById(id);
            if (entity1 != null){
                boolean bl = true;
                if (entity instanceof PlayerEntity){ bl = !((PlayerEntity) entity).abilities.creativeMode; }
                ((ChainKnotEntity) entity1).detachChain(true, bl, this);
            }
        }
        this.dropStack(new ItemStack(ConnectibleChains.TEMP_CHAIN, this.getHoldingEntities().size()));
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
