package com.legoatoom.develop.enitity;

import com.legoatoom.develop.ConnectibleChains;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChainKnotEntity extends AbstractDecorationEntity {

    private List<PlayerEntity> builders;
    private List<Integer> buildersID;

    private List<ChainKnotEntity> connections;
    private List<Integer> connectionsID;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) { super(entityType, world); }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ConnectibleChains.CHAIN_KNOT, world, pos);
        this.updatePosition((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
        this.setBoundingBox(new Box(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
        this.teleporting = true;
        this.builders = new ArrayList<>();
        this.buildersID = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.connectionsID = new ArrayList<>();
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
        if (!this.world.isClient){
            this.updateChain();
        }
    }

    protected void updateChain() {
        //TODO: what is the use of leashTag
        for (PlayerEntity playerEntity : this.builders){
            if (!this.isAlive() || !playerEntity.isAlive()){
                //this.detachLeash(true, true);
            }
        }
    }



    @Override
    public int getWidthPixels() { return 9; }

    @Override
    public int getHeightPixels() { return 9; }

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
    public boolean canStayAttached() {
        return this.world.getBlockState(this.attachmentPos).getBlock().isIn(BlockTags.FENCES);
    }

    /**
     * @see LeashKnotEntity#getOrCreate(World, BlockPos);
     * Basicly the same, so I cannot take credit.
     */
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, boolean mustExist) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box((double)i - 1.0D, (double)j - 1.0D, (double)k - 1.0D, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D));
        Iterator<ChainKnotEntity> var6 = list.iterator();

        ChainKnotEntity chainKnotEntity;
        do {
            if (!var6.hasNext()) {
                if (mustExist){
                    return null;
                }
                ChainKnotEntity chainKnotEntity1 = new ChainKnotEntity(world, pos);
                world.spawnEntity(chainKnotEntity1);
                chainKnotEntity1.onPlace();
                return chainKnotEntity1;
            }

            chainKnotEntity = var6.next();
        } while(!chainKnotEntity.getDecorationBlockPos().equals(pos));
        return chainKnotEntity;
    }

    @Override
    public void onPlace() { this.playSound(SoundEvents.BLOCK_CHAIN_STEP, 1.0F, 1.0F); }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.getType(), 0, this.getDecorationBlockPos());
    }

    /**
     * Connect a chain to a {@link PlayerEntity} or a different {@link ChainKnotEntity}.
     * @param entity
     * @param setter
     * @return <code>True</code> if a chain was successfully made between the entity and this ChainKnot,
     * else <code>False</code>
     */
    public boolean attachChain(Entity entity, PlayerEntity setter){
        if (entity instanceof PlayerEntity) {
            if (builders == null){
                this.builders = new ArrayList<>();
            }
            builders.add((PlayerEntity) entity);
        } else if (entity instanceof ChainKnotEntity) {
            if (entity.equals(this)) {
                return false;
            }
            if (connections == null){
                this.connections = new ArrayList<>();
            }
            connections.add((ChainKnotEntity) entity);
        } else {
            LOGGER.error("Entity of type: " + entity.getEntityName() +
                    " trying to connect to chain. Only Players and Other ChainsKnots allowed");
            return false;
        }

        if (!this.world.isClient && this.world instanceof ServerWorld) {
            ((ServerWorld)this.world).getChunkManager()
                    .sendToOtherNearbyPlayers(this,
                            new EntityAttachS2CPacket(this, entity));
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    public void setNewConnectorID(int id) {
        this.connectionsID.add(id);
        this.detachChain(false, false);
    }

    @Environment(EnvType.CLIENT)
    public void setNewBuilderID(int id) {
        this.buildersID.add(id);
    }

    public void detachChain(boolean sendPacket, boolean b1, int fromID) {
        Entity entity = this.world.getEntityById(fomrID){

        }
    }

    public List<PlayerEntity> getBuilders(){
        for (int ID : this.buildersID){
            this.builders.add((PlayerEntity) this.world.getEntityById(ID));
        }
        return this.builders;
    }

    public List<ChainKnotEntity> getConnections(){
        for (int ID : this.connectionsID){
            this.connections.add((ChainKnotEntity) this.world.getEntityById(ID));
        }
        return this.connections;
    }
}
