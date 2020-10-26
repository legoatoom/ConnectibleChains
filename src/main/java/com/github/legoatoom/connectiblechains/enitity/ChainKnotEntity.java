package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import org.lwjgl.system.CallbackI;

import java.util.*;
import java.util.stream.Stream;

public class ChainKnotEntity extends AbstractDecorationEntity {

    public static final double MAX_RANGE = 7d;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        this.updatePosition((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
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
        boolean b = false;
        ListTag listTag = new ListTag();
        for (Entity entity : this.holdingEntities.values()){
            if (entity != null){
                CompoundTag compoundTag = new CompoundTag();
                if (entity instanceof PlayerEntity){
                    UUID uuid = entity.getUuid();
                    compoundTag.putUuid("UUID", uuid);
                    b = true;
                } else if (entity instanceof AbstractDecorationEntity) {
                    BlockPos blockPos = ((AbstractDecorationEntity) entity).getDecorationBlockPos();
                    compoundTag.putInt("X", blockPos.getX());
                    compoundTag.putInt("Y", blockPos.getY());
                    compoundTag.putInt("Z", blockPos.getZ());
                    b = true;
                }
                listTag.add(compoundTag);
            }
        }
        if (b){
            tag.put("Chains", listTag);
        } else if (chainTags != null && !chainTags.isEmpty()) {
            tag.put("Chains", chainTags.copy());
        }
//        if (!this.holdingEntities.isEmpty()){
//            CompoundTag compoundTag = new CompoundTag();
//            if (this.holdingEntity instanceof PlayerEntity){
//                UUID uuid = this.holdingEntity.getUuid();
//                compoundTag.putUuid("UUID", uuid);
//            } else if (this.holdingEntity instanceof AbstractDecorationEntity) {
//                BlockPos blockPos = ((AbstractDecorationEntity) this.holdingEntity).getDecorationBlockPos();
//                compoundTag.putInt("X", blockPos.getX());
//                compoundTag.putInt("Y", blockPos.getY());
//                compoundTag.putInt("Z", blockPos.getZ());
//            }
//
//            tag.put("Chain", compoundTag);
//        } else if (this.chainTag != null){
//            tag.put("Chain", this.chainTag.copy());
//        }
    }

    public void readCustomDataFromTag(CompoundTag tag) {
        if (tag.contains("Chains")){
            this.chainTags = tag.getList("Chains", 10);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.world.isClient()){
            this.updateChains();
        }
    }

    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            boolean bl = false;
            List<ChainKnotEntity> list = this.world.getNonSpectatingEntities(ChainKnotEntity.class, new Box(this.getX() - MAX_RANGE, this.getY() - MAX_RANGE, this.getZ() - MAX_RANGE, this.getX() + MAX_RANGE, this.getY() + MAX_RANGE, this.getZ() + MAX_RANGE));
            Iterator<ChainKnotEntity> var7 = list.iterator();

            ChainKnotEntity mobEntity2;
            while (var7.hasNext()) {
                mobEntity2 = var7.next();
                ArrayList<Entity> holdings = mobEntity2.getHoldingEntities();
                if (holdings.contains(player) && !holdings.contains(this) && !mobEntity2.equals(this)) {
                    mobEntity2.attachChain(this, true, player.getEntityId());
                    bl = true;
                }
            }
            if (bl){
                onPlace();
            }

            if (!bl && player.getStackInHand(hand).getItem().equals(Items.CHAIN)) {
                if (this.getHoldingEntities().contains(player)){
                    onBreak(null);
                    detachChain(player, true, false);
                    if(!player.isCreative()){
                        player.getStackInHand(hand).increment(1);
                    }
                } else {
                    onPlace();
                    attachChain(player, true, 0);
                    if (!player.isCreative()) {
                        player.getStackInHand(hand).decrement(1);
                    }
                }
            }

            return ActionResult.CONSUME;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean bool = super.damage(source, amount);
        ArrayList<Entity> list = this.getHoldingEntities();
        for (Entity entity : list){
            if (!this.world.isClient()) {
                if (entity instanceof ChainKnotEntity && ((ChainKnotEntity) entity).holdersCount <= 1 && ((ChainKnotEntity) entity).getHoldingEntities().isEmpty()) {
                    entity.remove();
                }
                Vec3d middle = middleOf(getPos(), entity.getPos());
                ItemEntity entity1 = new ItemEntity(world, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
                entity1.setToDefaultPickupDelay();
                this.world.spawnEntity(entity1);
            }
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

    private final Map<Integer, Entity> holdingEntities = new HashMap<>();
    public int holdersCount = 0;

    private ListTag chainTags;

//    @Deprecated
//    @Nullable
//    private Entity holdingEntity;
//    @Deprecated
//    private int holdingEntityId;
//
//    @Deprecated
//    @Nullable
//    private CompoundTag chainTag;
//
//    @Deprecated
//    @Nullable
//    public Entity getHoldingEntity(){
//        if (this.holdingEntity == null && this.holdingEntityId != 0 && this.world.isClient()){
//            this.holdingEntity = this.world.getEntityById(this.holdingEntityId);
//        }
//        return this.holdingEntity;
//    }

    public ArrayList<Entity> getHoldingEntities(){
        if (this.world.isClient()) {
            for (Integer id : holdingEntities.keySet()) {
                if (id != 0 && holdingEntities.get(id) == null){
                    holdingEntities.put(id, this.world.getEntityById(id));
                }
            }
        }
        return new ArrayList<>(holdingEntities.values());
    }

    protected void updateChains(){
        if (chainTags != null) {
            ListTag copy = chainTags.copy();
            for (Tag tag : copy) {
                assert tag instanceof CompoundTag;
                this.deserializeChainTag(((CompoundTag) tag));
            }
        }

        Entity[] entitySet = holdingEntities.values().toArray(new Entity[0]).clone();
        for (Entity entity : entitySet){
            if (entity != null) {
                if (!this.isAlive() || !entity.isAlive() || entity.getPos().squaredDistanceTo(this.getPos()) > MAX_RANGE * MAX_RANGE) {
                    this.detachChain(entity, true, true);
                    onBreak(null);
                }
            }
        }
    }

//    @Deprecated
//    protected void updateChain(){
//        if (this.chainTag != null){
//            this.deserializeChainTag();
//        }
//
//        if (this.holdingEntity != null){
//            if (!this.isAlive() || !this.holdingEntity.isAlive()){
//                this.detachChain(true, true);
//            } else if (this.holdingEntity.getPos().squaredDistanceTo(this.getPos()) > MAX_RANGE*MAX_RANGE){
//                this.detachChain(true, true);
//            }
//        }
//    }

    public void detachChain(Entity entity, boolean sendPacket, boolean dropItem) {
        if (entity != null){
            if (this.holdingEntities.size() <= 1){
                this.teleporting = false;
            }
            if (entity instanceof ChainKnotEntity){
                if (((ChainKnotEntity) entity).holdingEntities.isEmpty()){
                    entity.teleporting = false;
                }
            }
            this.holdingEntities.remove(entity.getEntityId());
            if (!this.world.isClient() && dropItem){
                Vec3d middle = middleOf(getPos(), entity.getPos());
                ItemEntity entity1 = new ItemEntity(world, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
                entity1.setToDefaultPickupDelay();
                this.world.spawnEntity(entity1);
            }

            if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld){
                if (entity instanceof ChainKnotEntity){
                    ((ChainKnotEntity) entity).holdersCount--;
                    if (this.holdersCount <= 0 && getHoldingEntities().isEmpty()){
                        this.remove();
                    }
                }
                sendDetachChainPacket(entity.getEntityId());
            }
        }
    }

//    @Deprecated
//    public void detachChain(boolean sendPacket, boolean dropItem){
//        if (this.holdingEntity != null){
//            this.teleporting = false;
//            if (!(this.holdingEntity instanceof PlayerEntity)){
//                this.holdingEntity.teleporting = false;
//            }
//
//            this.holdingEntity = null;
//            this.chainTag = null;
//            if (!this.world.isClient() && dropItem){
//                this.dropItem(Items.CHAIN);
//            }
//
//            if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld){
//                sendAttachChainPacket(0);
//
//            }
//        }
//    }

    public void attachChain(Entity entity, boolean sendPacket, int fromPlayerEntityId){
        this.holdingEntities.put(entity.getEntityId(), entity);
        this.teleporting = true;
        if (!(entity instanceof PlayerEntity)){
            entity.teleporting = true;
        }

        if (fromPlayerEntityId != 0){
            removePlayerWithId(fromPlayerEntityId);
        }

        if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld){
            if (entity instanceof ChainKnotEntity){
                ((ChainKnotEntity) entity).holdersCount++;
            }
            sendAttachChainPacket(entity.getEntityId(), fromPlayerEntityId);
        }
    }

//    public void attachChain(Entity entity, boolean sendPacket){
//        this.holdingEntity = entity;
//        this.chainTag = null;
//        this.teleporting = true;
//        if (!(this.holdingEntity instanceof PlayerEntity)){
//            this.holdingEntity.teleporting = true;
//        }
//
//        if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld){
//            sendAttachChainPacket(this.holdingEntity.getEntityId());
//        }
//    }

    public void sendDetachChainPacket(int entityId){
        Stream<PlayerEntity> watchingPlayers = PlayerStream.around(world, getBlockPos(), 1024d);
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeIntArray(new int[]{this.getEntityId(), entityId});

        watchingPlayers.forEach(playerEntity ->
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, NetworkingPackages.S2C_CHAIN_DETACH_PACKET_ID, passedData));
    }

    public void sendAttachChainPacket(int entityId, int fromPlayerEntityId) {
        Stream<PlayerEntity> watchingPlayers = PlayerStream.around(world, getBlockPos(), 1024d);
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeIntArray(new int[]{this.getEntityId(), entityId});
        passedData.writeInt(fromPlayerEntityId);

        watchingPlayers.forEach(playerEntity ->
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, NetworkingPackages.S2C_CHAIN_ATTACH_PACKET_ID, passedData));
    }

    public void sendAttachChainPacket(int[] entityIds){
        Stream<PlayerEntity> watchingPlayers = PlayerStream.around(world, getBlockPos(), 1024d);
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeInt(this.getEntityId());
        passedData.writeIntArray(entityIds);

        watchingPlayers.forEach(playerEntity ->
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, NetworkingPackages.S2C_MULTI_CHAIN_ATTACH_PACKET_ID, passedData));
    }

    public void sendDetachChainPacket(int[] entityIds){
        Stream<PlayerEntity> watchingPlayers = PlayerStream.around(world, getBlockPos(), 1024d);
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeInt(this.getEntityId());
        passedData.writeIntArray(entityIds);

        watchingPlayers.forEach(playerEntity ->
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerEntity, NetworkingPackages.S2C_MULTI_CHAIN_DETACH_PACKET_ID, passedData));
    }

    @Environment(EnvType.CLIENT)
    public void addHoldingEntityId(int id, int fromPlayerId){
        if (fromPlayerId != 0){
            this.holdingEntities.remove(fromPlayerId);
        }
        this.holdingEntities.put(id, null);
    }

    @Environment(EnvType.CLIENT)
    public void removeHoldingEntityId(int id){
        this.holdingEntities.remove(id);
    }

    @Environment(EnvType.CLIENT)
    public void addHoldingEntityIds(int[] ids){
        for(int id : ids) this.holdingEntities.put(id, null);
    }

    @Environment(EnvType.CLIENT)
    public void removeHoldingEntityIds(int[] ids){
        for (int id: ids) this.holdingEntities.remove(id);
    }

    public void removePlayerWithId(int entityId) {
        this.holdingEntities.remove(entityId);
    }

//    @Deprecated
//    @Environment(EnvType.CLIENT)
//    public void setHoldingEntityId(int id){
//        this.holdingEntityId = id;
//        this.detachChain(false, false);
//    }

    private void deserializeChainTag(CompoundTag tag){
        if (tag != null && this.world instanceof ServerWorld){
            if (tag.contains("UUID")){
                UUID uuid = tag.getUuid("UUID");
                Entity entity = ((ServerWorld) this.world).getEntity(uuid);
                if (entity != null){
                    this.attachChain(entity, true, 0);
                    this.chainTags.remove(tag);
                    return;
                }
            } else if (tag.contains("X")){
                BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.world, blockPos, true);
                if (entity != null) {
                    this.attachChain(ChainKnotEntity.getOrCreate(this.world, blockPos), true, 0);
                    this.chainTags.remove(tag);
                }
                return;
            }

            // At the start the server and client need to tell each other the info.
            // So we need to check if the object is old enough for these things to exist before we delete them.
            if (this.age > 100) {
                this.dropItem(Items.CHAIN);
                this.chainTags.remove(tag);
            }
        }
    }



//    private void deserializeChainTag(){
//        if (this.chainTag != null && this.world instanceof ServerWorld){
//                if (this.chainTag.containsUuid("UUID")){
//                    UUID uuid = this.chainTag.getUuid("UUID");
//                    Entity entity = ((ServerWorld) this.world).getEntity(uuid);
//                    if (entity != null){
//                        this.attachChain(entity, true);
//                        return;
//                    }
//                } else if (this.chainTag.contains("X")){
//                    BlockPos blockPos = new BlockPos(chainTag.getInt("X"), this.chainTag.getInt("Y"), this.chainTag.getInt("Z"));
//                    ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.world, blockPos, true);
//                    if (entity != null) {
//                        this.attachChain(ChainKnotEntity.getOrCreate(this.world, blockPos), true);
//                    }
//                    return;
//                }
//
//                // At the start the server and client need to tell each other the info.
//                // So we need to check if the object is old enough for these things to exist before we delete them.
//                if (this.age > 100) {
//                    this.dropItem(Items.CHAIN);
//                    this.chainTag = null;
//                }
//        }
//    }

    public static Vec3d middleOf(Vec3d a, Vec3d b){
        double x = (a.getX() - b.getX())/2d + b.getX();
        double y = (a.getY() - b.getY())/2d + b.getY();
        double z = (a.getZ() - b.getZ())/2d + b.getZ();
        return new Vec3d(x, y, z);
    }
}
