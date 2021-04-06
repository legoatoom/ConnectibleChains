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
import com.github.legoatoom.connectiblechains.util.Helper;
import com.github.legoatoom.connectiblechains.util.NetworkingPackages;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.Block;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has connections between others of it's kind, and is a combination of {@link net.minecraft.entity.mob.MobEntity}
 * and {@link net.minecraft.entity.decoration.LeashKnotEntity}.
 *
 * @author legoatoom
 */
public class ChainKnotEntity extends AbstractDecorationEntity {

    /**
     * The max range of the chain.
     */
    private static final double MAX_RANGE = 7d;

    /**
     * The distance when it is visible.
     */
    public static final double VISIBLE_RANGE = 2048.0D;

    /**
     * A map that holds a list of entity ids. These entities should be {@link ChainCollisionEntity ChainCollisionEntities}
     * The key is the entity id of the ChainKnot that this is connected to.
     */
    private final Map<Integer, ArrayList<Integer>> COLLISION_STORAGE;

    /**
     * A map of entities that this chain is connected to.
     * The entity can be a {@link PlayerEntity} or a {@link ChainKnotEntity}.
     */
    private final Map<Integer, Entity> holdingEntities = new HashMap<>();

    /**
     * A counter of how many other chainsKnots connect to this.
     */
    private int holdersCount = 0;

    /**
     * The Tag that stores everything
     */
    private ListTag chainTags;

    /**
     * A timer integer for destroying this entity if it isn't connected anything.
     */
    private int obstructionCheckCounter;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) {
        super(entityType, world);
        this.COLLISION_STORAGE = new HashMap<>();
    }

    public ChainKnotEntity(World world, BlockPos pos) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        this.updatePosition((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.setBoundingBox(new Box(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
        this.teleporting = true;
        this.COLLISION_STORAGE = new HashMap<>();
    }

    /**
     * This method tries to check if around the target there are other {@link ChainKnotEntity ChainKnotEntities} that
     * have a connection to this player, if so we make a chainKnot at the location and connect the chain to it and remove
     * the connection to the player.
     *
     * @param playerEntity the player wo tries to make a connection.
     * @param world The current world.
     * @param pos the position where we want to make a chainKnot.
     * @param chain nullable chainKnot if one already has one, if null, we will make one only if we have to make a connection.
     * @return boolean, if it has made a connection.
     */
    public static Boolean tryAttachHeldChainsToBlock(PlayerEntity playerEntity, World world, BlockPos pos, @Nullable ChainKnotEntity chain) {
        boolean hasMadeConnection = false;
        double i = pos.getX();
        double j = pos.getY();
        double k = pos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class,
                new Box(i - MAX_RANGE, j - MAX_RANGE, k - MAX_RANGE,
                        i + MAX_RANGE, j + MAX_RANGE, k + MAX_RANGE));

        for (ChainKnotEntity otherKnots : list) {
            if (otherKnots.getHoldingEntities().contains(playerEntity)) {
                if (!otherKnots.equals(chain)) {
                    // We found a knot that is connected to the player and therefore needs to connect to the chain.
                    if (chain == null) {
                        chain = new ChainKnotEntity(world, pos);
                        world.spawnEntity(chain);
                        chain.onPlace();
                    }

                    otherKnots.attachChain(chain, true, playerEntity.getEntityId());
                    hasMadeConnection = true;
                }
            }
        }
        return hasMadeConnection;
    }

    /**
     * This entity does not want to set a facing.
     */
    public void setFacing(Direction facing) {
    }

    /**
     * Update the position of this chain to the position of the block this is attached too.
     */
    protected void updateAttachmentPosition() {
        this.setPos((double) this.attachmentPos.getX() + 0.5D, (double) this.attachmentPos.getY() + 0.5D, (double) this.attachmentPos.getZ() + 0.5D);
        this.setBoundingBox(new Box(this.getX() - 0.1875D, this.getY() - 0.25D + 0.125D, this.getZ() - 0.1875D, this.getX() + 0.1875D, this.getY() + 0.25D + 0.125D, this.getZ() + 0.1875D));
    }

    /**
     * This happens every tick.
     * It deletes the chainEntity if it is in the void.
     * It updates the chains, see {@link #updateChains()}
     * It checks if it is still connected to a block every 100 ticks.
     */
    @Override
    public void tick() {
        if (!this.world.isClient) {
            if (this.getY() < -64.0D) {
                this.destroy();
            }
            this.updateChains();

            if (this.obstructionCheckCounter++ == 100) {
                this.obstructionCheckCounter = 0;
                if (!this.removed && !this.canStayAttached()) {
                    ArrayList<Entity> list = this.getHoldingEntities();
                    for (Entity entity : list) {
                        if (entity instanceof ChainKnotEntity) {
                            damageLink(false, (ChainKnotEntity) entity);
                        }
                    }
                    this.remove();
                    this.onBreak(null);
                }
            }
        }
    }

    /**
     * Simple checker to see if the block is connected to a fence or a wall.
     * @return boolean - if it can stay attached.
     */
    public boolean canStayAttached() {
        Block block = this.world.getBlockState(this.attachmentPos).getBlock();
        return canConnectTo(block);
    }

    /**
     * Is this block acceptable to connect too?
     * @param block the block in question.
     * @return boolean if is allowed or not.
     */
    public static boolean canConnectTo(Block block){
        return block.isIn(BlockTags.WALLS) || block.isIn(BlockTags.FENCES);
    }

    /**
     * When this entity is being attacked, we remove all connections and then remove this entity.
     * @return if it is successfully attacked.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.world.isClient && !this.removed) {
            Entity sourceEntity = source.getAttacker();
            if (sourceEntity instanceof PlayerEntity) {
                boolean isCreative = ((PlayerEntity) sourceEntity).isCreative();
                if (!((PlayerEntity) sourceEntity).getMainHandStack().isEmpty()
                        && ((PlayerEntity) sourceEntity).getMainHandStack().getItem().isIn(FabricToolTags.SHEARS)) {
                    ArrayList<Entity> list = this.getHoldingEntities();
                    for (Entity entity : list) {
                        if (entity instanceof ChainKnotEntity) {
                            damageLink(isCreative, (ChainKnotEntity) entity);
                        }
                    }
                    this.onBreak(null);
                    this.remove();
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * Method to write all connections in a {@link CompoundTag} when we save the game.
     * It doesn't store the {@link #holdersCount} or {@link #COLLISION_STORAGE} since
     * they will be updated when connection are being remade when we read it.
     *
     * @param tag the tag to write info in.
     */
    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        boolean b = false;
        ListTag listTag = new ListTag();
        for (Entity entity : this.holdingEntities.values()) {
            if (entity != null) {
                CompoundTag compoundTag = new CompoundTag();
                if (entity instanceof PlayerEntity) {
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
        if (b) {
            tag.put("Chains", listTag);
        } else if (chainTags != null && !chainTags.isEmpty()) {
            tag.put("Chains", chainTags.copy());
        }
    }

    /**
     * Read all the info into the {@link #chainTags}
     * We do not make connections here because not all entities might be loaded yet.
     *
     * @param tag the tag to read from.
     */
    public void readCustomDataFromTag(CompoundTag tag) {
        if (tag.contains("Chains")) {
            this.chainTags = tag.getList("Chains", 10);
        }
    }

    public int getWidthPixels() {
        return 9;
    }

    public int getHeightPixels() {
        return 9;
    }

    public void onBreak(@Nullable Entity entity) {
        this.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0F, 1.0F);
    }

    public void onPlace() {
        this.playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
    }

    public void updatePosition(double x, double y, double z) {
        super.updatePosition((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D, (double) MathHelper.floor(z) + 0.5D);
    }

    /**
     * This method will call {@link #deserializeChainTag(CompoundTag)} if the {@link #chainTags} has any tags.
     * It will also break all connections that are larger than the {@link #MAX_RANGE}
     */
    protected void updateChains() {
        if (chainTags != null) {
            ListTag copy = chainTags.copy();
            for (Tag tag : copy) {
                assert tag instanceof CompoundTag;
                this.deserializeChainTag(((CompoundTag) tag));
            }
        }

        Entity[] entitySet = holdingEntities.values().toArray(new Entity[0]).clone();
        for (Entity entity : entitySet) {
            if (entity != null) {
                if (!this.isAlive() || !entity.isAlive() || entity.getPos().squaredDistanceTo(this.getPos()) > MAX_RANGE * MAX_RANGE) {
                    this.detachChain(entity, true,  !(entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()));
                    onBreak(null);
                }
            }
        }
    }

    /**
     * Get method for all the entities that we are connected to.
     * @return ArrayList with Entities - These entities are {@link PlayerEntity PlayerEntities} or {@link ChainKnotEntity ChainKnotEntities}
     */
    public ArrayList<Entity> getHoldingEntities() {
        if (this.world.isClient()) {
            for (Integer id : holdingEntities.keySet()) {
                if (id != 0 && holdingEntities.get(id) == null) {
                    holdingEntities.put(id, this.world.getEntityById(id));
                }
            }
        }
        return new ArrayList<>(holdingEntities.values());
    }

    /**
     * Destroy the collisions between two chains, and delete the endpoint if it doesn't have any other connection.
     * @param doNotDrop if we should not drop an item.
     * @param endChain the entity that this is connected to.
     */
    public void damageLink(boolean doNotDrop, ChainKnotEntity endChain) {
        if (!this.getHoldingEntities().contains(endChain)) return; // We cannot destroy a connection that does not exist.
        if (endChain.holdersCount <= 1 && endChain.getHoldingEntities().isEmpty()) {
            endChain.remove();
        }
        this.deleteCollision(endChain);
        this.detachChain(endChain, true,  !doNotDrop);
        onBreak(null);
    }

    /**
     * This method tries to connect to an entity that is in the {@link #chainTags}.
     * If they do not exist yet, we skip them. If they do, make a connection and remove it from the tag.
     *
     * If when the {@link #age} of this entity is bigger than 100, we remove the tag from the {@link #chainTags}
     * meaning that we cannot find the connection anymore and we assume that it will not be loaded in the future.
     * @see #updateChains()
     * @param tag the tag that contains a single connection.
     */
    private void deserializeChainTag(CompoundTag tag) {
        if (tag != null && this.world instanceof ServerWorld) {
            if (tag.contains("UUID")) {
                UUID uuid = tag.getUuid("UUID");
                Entity entity = ((ServerWorld) this.world).getEntity(uuid);
                if (entity != null) {
                    this.attachChain(entity, true, 0);
                    this.chainTags.remove(tag);
                    return;
                }
            } else if (tag.contains("X")) {
                BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
                ChainKnotEntity entity = ChainKnotEntity.getOrCreate(this.world, blockPos, true);
                if (entity != null) {
                    this.attachChain(ChainKnotEntity.getOrCreate(this.world, blockPos, false), true, 0);
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

    /**
     * Attach this chain to an entity.
     * @param entity The entity to connect to.
     * @param sendPacket Whether we send a packet to the client.
     * @param fromPlayerEntityId the entityID of the player that this connects to. 0 if it a chainKnot.
     */
    public void attachChain(Entity entity, boolean sendPacket, int fromPlayerEntityId) {
        this.holdingEntities.put(entity.getEntityId(), entity);
        this.teleporting = true;
        if (!(entity instanceof PlayerEntity)) {
            entity.teleporting = true;
        }

        if (fromPlayerEntityId != 0) {
            removePlayerWithId(fromPlayerEntityId);
        }

        if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld) {
            if (entity instanceof ChainKnotEntity) {
                ((ChainKnotEntity) entity).holdersCount++;
                createCollision(entity);
            }
            sendAttachChainPacket(entity.getEntityId(), fromPlayerEntityId);
        }
    }

    /**
     * Remove a link between this chainKnot and a player or other chainKnot.
     * @param entity the entity it is connected to.
     * @param sendPacket should we send a packet to the client?
     * @param dropItem should we drop an item?
     */
    public void detachChain(Entity entity, boolean sendPacket, boolean dropItem) {
        if (entity != null) {
            if (this.holdingEntities.size() <= 1) {
                this.teleporting = false;
            }
            if (entity instanceof ChainKnotEntity) {
                if (((ChainKnotEntity) entity).holdingEntities.isEmpty()) {
                    entity.teleporting = false;
                }
            }

            this.holdingEntities.remove(entity.getEntityId());
            if (!this.world.isClient() && dropItem && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                Vec3d middle = Helper.middleOf(getPos(), entity.getPos());
                ItemEntity entity1 = new ItemEntity(world, middle.x, middle.y, middle.z, new ItemStack(Items.CHAIN));
                entity1.setToDefaultPickupDelay();
                this.world.spawnEntity(entity1);
            }

            if (!this.world.isClient() && sendPacket && this.world instanceof ServerWorld) {
                if (entity instanceof ChainKnotEntity) {
                    ((ChainKnotEntity) entity).holdersCount--;
                    if (this.holdersCount <= 0 && getHoldingEntities().isEmpty()) {
                        this.remove();
                    }
                }
                deleteCollision(entity);
                sendDetachChainPacket(entity.getEntityId());
            }
        }
    }

    /**
     * Create a collision between this and an entity.
     * It spawns multiple {@link ChainCollisionEntity ChainCollisionEntities} that are equal distance from each other.
     * Position is the same no matter what if the connection is from A -> B or A <- B.
     *
     * @see ChainCollisionEntity
     * @param entity the entity to create collisions too.
     */
    private void createCollision(Entity entity) {
        double distance = this.getDecorationBlockPos().getManhattanDistance(entity.getBlockPos());
        double a = .5 / distance;
        double v = a;

        ArrayList<Integer> entityIdList = new ArrayList<>();
        double x, y, z;
        double offset = 0.2D;
        while (v <= 1 - (a/2)) {
            x = MathHelper.lerp(v, this.getX(), entity.getX());
            y = MathHelper.lerp(v, this.getY(), entity.getY()) + Helper.drip(v * distance, distance) + offset;
            z = MathHelper.lerp(v, this.getZ(), entity.getZ());
            ChainCollisionEntity c = new ChainCollisionEntity(this.world, x - .15, y, z - .15, this.getEntityId(), entity.getEntityId());

            if (world.spawnEntity(c)) {
                entityIdList.add(c.getEntityId());
            } else {
                LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            }
            v = v + a;
        }
        this.COLLISION_STORAGE.put(entity.getEntityId(), entityIdList);
    }

    /**
     * Remove a collision between this and an entity.
     * @param entity the entity in question.
     */
    private void deleteCollision(Entity entity) {
        int entityId = entity.getEntityId();
        ArrayList<Integer> entityIdList = this.COLLISION_STORAGE.get(entityId);
        if (entityIdList != null) {
            entityIdList.forEach(id -> {
                Entity e = world.getEntityById(id);
                if (e instanceof ChainCollisionEntity) {
                    e.remove();
                }
            });
        }
        this.COLLISION_STORAGE.remove(entityId);
    }

    /**
     * Get or create a chainKnot in a location.
     * @param world the world.
     * @param pos the location to check.
     * @param hasToExist boolean that specifies if the chainKnot has to exist, if this is true and we cannot find
     *                   a knot, it will return null.
     * @return {@link ChainKnotEntity} or null
     */
    @Nullable
    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, Boolean hasToExist) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        final List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class,
                new Box((double) posX - 1.0D, (double) posY - 1.0D, (double) posZ - 1.0D,
                        (double) posX + 1.0D, (double) posY + 1.0D, (double) posZ + 1.0D));
        Iterator<ChainKnotEntity> var6 = list.iterator();

        ChainKnotEntity surroundingChains;
        do {
            if (!var6.hasNext()) {
                if (hasToExist) {
                    // If it has to exist and it doesn't, we return null.
                    return null;
                }
                ChainKnotEntity newChain = new ChainKnotEntity(world, pos);
                world.spawnEntity(newChain);
                newChain.onPlace();
                return newChain;
            }

            surroundingChains = var6.next();
        } while (surroundingChains == null || !surroundingChains.getDecorationBlockPos().equals(pos));

        return surroundingChains;
    }

    /**
     * Send to all players around that this chain wants to attach to another entity.
     * @param entityId the entity to connect to.
     * @param fromPlayerEntityId the {@link PlayerEntity} id that made the connection.
     */
    public void sendAttachChainPacket(int entityId, int fromPlayerEntityId) {
        Stream<ServerPlayerEntity> watchingPlayers =
                PlayerLookup.around((ServerWorld) world, getBlockPos(), VISIBLE_RANGE).stream();
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeIntArray(new int[]{this.getEntityId(), entityId});
        passedData.writeInt(fromPlayerEntityId);

        watchingPlayers.forEach(playerEntity ->
                ServerPlayNetworking.send(playerEntity,
                        NetworkingPackages.S2C_CHAIN_ATTACH_PACKET_ID, passedData));
    }

    /**
     * Send a package to all the clients around this entity that specifies it want's to detach.
     * @param entityId the entity id that it wants to connect to.
     */
    private void sendDetachChainPacket(int entityId) {
        assert !this.world.isClient;

        Stream<ServerPlayerEntity> watchingPlayers =
                PlayerLookup.around((ServerWorld) world, getBlockPos(), VISIBLE_RANGE).stream();
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());

        //Write our id and the id of the one we connect to.
        passedData.writeIntArray(new int[]{this.getEntityId(), entityId});

        watchingPlayers.forEach(playerEntity ->
                ServerPlayNetworking.send(playerEntity,
                        NetworkingPackages.S2C_CHAIN_DETACH_PACKET_ID, passedData));
    }

    /**
     * Remove a player id from the {@link #holdingEntities list}
     * @param playerId the id of the player.
     */
    private void removePlayerWithId(int playerId) {
        this.holdingEntities.remove(playerId);
    }

    /**
     * Should we render this?
     * @param distance the distance from the chainKnot.
     * @return boolean, yes or no.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        return distance < VISIBLE_RANGE;
    }

    /**
     * Interaction of a player and this entity.
     * It will try to make new connections to the player or allow other chains that are connected to the player to
     * be made to this.
     *
     * @param player the player that interacted.
     * @param hand the hand of the player.
     * @return ActionResult
     */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            boolean madeConnection = tryAttachHeldChainsToBlock(player, world, getDecorationBlockPos(), this);

            if (!madeConnection) {
                if (this.getHoldingEntities().contains(player)) {
                    onBreak(null);
                    detachChain(player, true, false);
                    if (!player.isCreative()) {
                        player.giveItemStack(new ItemStack(Items.CHAIN));
                    }
                } else if (player.getStackInHand(hand).getItem().equals(Items.CHAIN)) {
                    onPlace();
                    attachChain(player, true, 0);
                    if (!player.isCreative()) {
                        player.getStackInHand(hand).decrement(1);
                    }
                } else {
                    damage(DamageSource.player(player), 0);
                }
            } else {
                onPlace();
            }

            return ActionResult.CONSUME;
        }
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return -0.0625F;
    }

    /**
     * Spawn entity package.
     *
     * @see EntitySpawnPacketCreator
     */
    @Override
    public Packet<?> createSpawnPacket() {
        Function<PacketByteBuf, PacketByteBuf> extraData = packetByteBuf -> packetByteBuf; // I have no extra data to send.
        return EntitySpawnPacketCreator.create(this, NetworkingPackages.S2C_SPAWN_CHAIN_KNOT_PACKET, extraData);
    }

    /**
     * Not sure what this does but {@link net.minecraft.entity.decoration.LeashKnotEntity#method_30951(float)} has it.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public Vec3d method_30951(float f) {
        return this.method_30950(f).add(0.0D, 0.2D, 0.0D);
    }

    /**
     * Client method to keep track of all the entities that it holds.
     * Adds an id that it holds, removes the player id if applicable.
     *
     * @param id the id that we connect to.
     * @param fromPlayerId the id from the player it was from, 0 if this was not applicable.
     * @see com.github.legoatoom.connectiblechains.client.ClientInitializer
     */
    @Environment(EnvType.CLIENT)
    public void addHoldingEntityId(int id, int fromPlayerId) {
        if (fromPlayerId != 0) {
            this.holdingEntities.remove(fromPlayerId);
        }
        this.holdingEntities.put(id, null);
    }

    /**
     * Client method to keep track of all the entities that it holds.
     * Removes a id that it holds.
     *
     * @param id the id that we do not connect to anymore.
     * @see com.github.legoatoom.connectiblechains.client.ClientInitializer
     */
    @Environment(EnvType.CLIENT)
    public void removeHoldingEntityId(int id) {
        this.holdingEntities.remove(id);
    }

    /**
     * Multiple version of {@link #addHoldingEntityId(int id, int playerFromId)}
     * This version does not have a playerFromId, since this only is called when the world is loaded and
     * all previous connection need to be remade.
     *
     * @see com.github.legoatoom.connectiblechains.mixin.server.world.ThreadedAnvilChunkStorageMixin
     * @param ids array of ids to connect to.
     */
    @Environment(EnvType.CLIENT)
    public void addHoldingEntityIds(int[] ids) {
        for (int id : ids) this.holdingEntities.put(id, null);
    }


}
