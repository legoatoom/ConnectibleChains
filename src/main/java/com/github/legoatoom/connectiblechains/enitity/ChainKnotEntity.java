package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import com.sun.org.apache.xpath.internal.operations.Mod;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ChainKnotEntity extends AbstractDecorationEntity {

    public static final double MAX_RANGE = 7d;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        this.updatePosition((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        float f = 0.125F;
        float g = 0.1875F;
        float h = 0.25F;
        this.setBoundingBox(new Box(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
        this.teleporting = true;
    }

    public void updatePosition(double x, double y, double z) {
        super.updatePosition((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D, (double) MathHelper.floor(z) + 0.5D);
    }

    protected void updateAttachmentPosition() {
        this.setPos((double) this.attachmentPos.getX() + 0.5D, (double) this.attachmentPos.getY() + 0.5D, (double) this.attachmentPos.getZ() + 0.5D);
        this.setBoundingBox(new Box(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
    }

    public void setFacing(Direction facing) {
    }

    public int getWidthPixels() {
        return 9;
    }

    public int getHeightPixels() {
        return 9;
    }

    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return -0.0625F;
    }

    @Environment(EnvType.CLIENT)
    public boolean shouldRender(double distance) {
        return distance < 1024.0D;
    }

    public void onBreak(@Nullable Entity entity) {
        this.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0F, 1.0F);
    }

    public void writeCustomDataToTag(CompoundTag tag) {
        if (this.holdingEntity != null){
            CompoundTag compoundTag = new CompoundTag();
            if (this.holdingEntity instanceof PlayerEntity){
                UUID uuid = this.holdingEntity.getUuid();
                compoundTag.putUuid("UUID", uuid);
            } else if (this.holdingEntity instanceof AbstractDecorationEntity) {
                BlockPos blockPos = ((AbstractDecorationEntity) this.holdingEntity).getDecorationBlockPos();
                compoundTag.putInt("X", blockPos.getX());
                compoundTag.putInt("Y", blockPos.getY());
                compoundTag.putInt("Z", blockPos.getZ());
            }

            tag.put("Chain", compoundTag);
        } else if (this.chainTag != null){
            tag.put("Chain", this.chainTag.copy());
        }
    }

    public void readCustomDataFromTag(CompoundTag tag) {
        if (tag.contains("Chain")){
            this.chainTag = tag.getCompound("Chain");
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient()){
            this.updateChain();
        }
    }

    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            boolean bl = false;
            double d = 7.0D;
            List<ChainKnotEntity> list = this.world.getNonSpectatingEntities(ChainKnotEntity.class, new Box(this.getX() - 7.0D, this.getY() - 7.0D, this.getZ() - 7.0D, this.getX() + 7.0D, this.getY() + 7.0D, this.getZ() + 7.0D));
            Iterator<ChainKnotEntity> var7 = list.iterator();

            ChainKnotEntity mobEntity2;
            if (!list.isEmpty()){
                onPlace();
            }
            while (var7.hasNext()) {
                mobEntity2 = var7.next();
                if (mobEntity2.getHoldingEntity() == player) {
                    mobEntity2.attachChain(this, true);
                    bl = true;
                }
            }

            if (!bl && player.getStackInHand(hand).getItem().equals(Items.CHAIN)) {
                if (!isChained()){
                    attachChain(player, true);
                    onPlace();
                } else {
                    ChainKnotEntity chainKnotEntity = getOrCreateWithoutConnection(world, getDecorationBlockPos());
                    chainKnotEntity.attachChain(player, true);
                }
                if(!player.isCreative()){
                    player.getStackInHand(hand).decrement(1);
                }
            }

            return ActionResult.CONSUME;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean bool = super.damage(source, amount);
        if (bool && this.getHoldingEntity() != null){
            this.dropItem(Items.CHAIN);
        }
        return bool;
    }

    public boolean canStayAttached() {
        return this.world.getBlockState(this.attachmentPos).getBlock().isIn(BlockTags.FENCES);
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos){
        return getOrCreate(world, pos, false);
    }

    @Nullable
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, Boolean hasToExist) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box((double) i - 1.0D, (double) j - 1.0D, (double) k - 1.0D, (double) i + 1.0D, (double) j + 1.0D, (double) k + 1.0D));
        Iterator<ChainKnotEntity> var6 = list.iterator();

        ChainKnotEntity leashKnotEntity;
        do {
            if (!var6.hasNext()) {
                if (hasToExist){
                    return null;
                }
                ChainKnotEntity leashKnotEntity2 = new ChainKnotEntity(world, pos);
                world.spawnEntity(leashKnotEntity2);
                leashKnotEntity2.onPlace();
                return leashKnotEntity2;
            }

            leashKnotEntity = var6.next();
        } while (leashKnotEntity == null || !leashKnotEntity.getDecorationBlockPos().equals(pos));

        return leashKnotEntity;
    }

    public static ChainKnotEntity getOrCreateWithoutConnection(World world, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box((double) i - 1.0D, (double) j - 1.0D, (double) k - 1.0D, (double) i + 1.0D, (double) j + 1.0D, (double) k + 1.0D));
        Iterator<ChainKnotEntity> var6 = list.iterator();

        ChainKnotEntity leashKnotEntity;
        do {
            if (!var6.hasNext()) {
                ChainKnotEntity leashKnotEntity2 = new ChainKnotEntity(world, pos);
                world.spawnEntity(leashKnotEntity2);
                leashKnotEntity2.onPlace();
                return leashKnotEntity2;
            }

            leashKnotEntity = var6.next();
        } while ((!leashKnotEntity.getDecorationBlockPos().equals(pos) || leashKnotEntity.isChained()));

        return leashKnotEntity;
    }

    public void onPlace() {
        this.playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
    }

    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.getType(), 0, this.getDecorationBlockPos());
    }

    @Environment(EnvType.CLIENT)
    public Vec3d method_30951(float f) {
        return this.method_30950(f).add(0.0D, 0.2D, 0.0D);
    }

    // ----- MobEntity PART OF CODE ----- //

    @Nullable
    private Entity holdingEntity;
    private int holdingEntityId;

    @Nullable
    private CompoundTag chainTag;

    @Nullable
    public Entity getHoldingEntity(){
        if (this.holdingEntity == null && this.holdingEntityId != 0 && this.world.isClient()){
            this.holdingEntity = this.world.getEntityById(this.holdingEntityId);
        }
        return this.holdingEntity;
    }

    protected void updateChain(){
        if (this.chainTag != null){
            this.deserializeChainTag();
        }

        if (this.holdingEntity != null){
            if (!this.isAlive() || !this.holdingEntity.isAlive()){
                this.detachChain(true, true);
            } else if (this.holdingEntity.getPos().squaredDistanceTo(this.getPos()) > MAX_RANGE*MAX_RANGE){
                this.detachChain(true, true);
            }
        }
    }

    public void detachChain(boolean sendPacket, boolean dropItem){
        if (this.holdingEntity != null){
            this.teleporting = false;
            if (!(this.holdingEntity instanceof PlayerEntity)){
                this.holdingEntity.teleporting = false;
            }

            this.holdingEntity = null;
            this.chainTag = null;
            if (!this.world.isClient() && dropItem){
                this.dropItem(Items.CHAIN);
            }

            if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld){
                sendAttachChainPacket(0);

            }
        }
    }

    public void attachChain(Entity entity, boolean sendPacket){
        this.holdingEntity = entity;
        this.chainTag = null;
        this.teleporting = true;
        if (!(this.holdingEntity instanceof PlayerEntity)){
            this.holdingEntity.teleporting = true;
        }

        if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld){
            sendAttachChainPacket(this.holdingEntity.getEntityId());
        }
    }

    public void sendAttachChainPacket(int entityId) {
        Stream<PlayerEntity> watchingPlayers = PlayerStream.around(world, getBlockPos(), 1024d);
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeIntArray(new int[]{this.getEntityId(), entityId});

        watchingPlayers.forEach(playerEntity ->
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, NetworkingPackages.S2C_CHAIN_ATTACH_PACKET_ID, passedData));
    }

    @Environment(EnvType.CLIENT)
    public void setHoldingEntityId(int id){
        this.holdingEntityId = id;
        this.detachChain(false, false);
    }

    public boolean isChained(){
        return this.holdingEntity != null;
    }

    private void deserializeChainTag(){
        if (this.chainTag != null && this.world instanceof ServerWorld){
                if (this.chainTag.containsUuid("UUID")){
                    UUID uuid = this.chainTag.getUuid("UUID");
                    Entity entity = ((ServerWorld) this.world).getEntity(uuid);
                    if (entity != null){
                        this.attachChain(entity, true);
                        return;
                    }
                } else if (this.chainTag.contains("X")){
                    BlockPos blockPos = new BlockPos(chainTag.getInt("X"), this.chainTag.getInt("Y"), this.chainTag.getInt("Z"));
                    ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.world, blockPos, true);
                    if (entity != null) {
                        this.attachChain(ChainKnotEntity.getOrCreate(this.world, blockPos), true);
                    }
                    return;
                }

                // At the start the server and client need to tell each other the info.
                // So we need to check if the object is old enough for these things to exist before we delete them.
                if (this.age > 100) {
                    this.dropItem(Items.CHAIN);
                    this.chainTag = null;
                }
        }
    }
}
