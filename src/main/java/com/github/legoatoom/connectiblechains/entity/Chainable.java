package com.github.legoatoom.connectiblechains.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Leashable;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.LeadItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * This class contains all the logic for chaining entities.
 * Inspired by Leashable, but this allows for multiple connections.
 *
 * @see Leashable
 */
public interface Chainable {
    String CHAINS_NBT_KEY = "Chains";
    String SOURCE_ITEM_KEY = "SourceItem";

    static double getMaxChainLength() {
        return ConnectibleChains.runtimeConfig.getMaxChainRange();
    }

    private static <E extends BlockAttachedEntity & Chainable> boolean canAttachTo(E entity, Entity potentialHolder) {
        if (entity.getChainData(potentialHolder) != null) {
            return false;
        } else if (potentialHolder instanceof Chainable chainable) {
            return !entity.equals(potentialHolder) && chainable.getChainData(entity) == null;
        }
        return false;
    }

    private static <E extends BlockAttachedEntity & Chainable> HashSet<ChainData> readChainDataSet(E entity, NbtCompound nbt) {
        HashSet<ChainData> result = new HashSet<>();
        if (nbt.contains(CHAINS_NBT_KEY, NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList(CHAINS_NBT_KEY, NbtElement.COMPOUND_TYPE);
            for (NbtElement element : list) {
                if (!(element instanceof NbtCompound compound)) continue;

                ChainData newChainData = null;
                Item source = Registries.ITEM.get(Identifier.tryParse(compound.getString(SOURCE_ITEM_KEY)));

                if (compound.containsUuid("UUID")) {
                    newChainData = new ChainData(Either.left(compound.getUuid("UUID")), source);
                } else if (compound.contains("DestX")) {
                    // Vanilla uses an NbtIntArray, but changing it here means would have to create a data-fixer, probably.
                    Either<UUID, BlockPos> either = Either.right(new BlockPos(compound.getInt("DestX"), compound.getInt("DestY"), compound.getInt("DestZ")));
                    newChainData = new ChainData(either, source);
                } else if (compound.contains("RelX")) {
                    // OLD DEPRECATED RELATIVE WAY OF STORING: Here for when people upgrade from previous versions. //
                    var relPos = new BlockPos(compound.getInt("RelX"), compound.getInt("RelY"), compound.getInt("RelZ"));
                    var desPos = relPos.add(entity.getAttachedBlockPos());
                    newChainData = new ChainData(Either.right(desPos), source);
                }

                if (newChainData != null) {
                    result.add(newChainData);
                }
            }
        }
        return result;
    }

    /**
     * Goes through all the data sets and resolves the unresolved data.
     */
    private static <E extends BlockAttachedEntity & Chainable> void resolveChainDataSet(E entity, HashSet<ChainData> chainDataSet) {
        // Sanity check for server world
        if (!(entity.getWorld() instanceof ServerWorld serverWorld)) return;

        for (ChainData chainData : new HashSet<>(chainDataSet)) {
            if (chainData.unresolvedChainData != null) {
                Optional<UUID> optionalUUID = chainData.unresolvedChainData.left();
                Optional<BlockPos> optionalBlockPos = chainData.unresolvedChainData.right();
                if (optionalUUID.isPresent()) {
                    Entity chainHolder = serverWorld.getEntity(optionalUUID.get());
                    if (chainHolder != null) {
                        ChainData newChainData = new ChainData(chainHolder, chainData.sourceItem);
                        entity.replaceChainData(chainData, null);
                        attachChain(entity, newChainData, null, true); // TODO: Do it in one bulk action instead of separate.
                        continue;
                    }
                } else if (optionalBlockPos.isPresent()) {
                    ChainKnotEntity chainHolder = ChainKnotEntity.getOrNull(serverWorld, optionalBlockPos.get());
                    if (chainHolder != null) {
                        ChainData newChainData = new ChainData(chainHolder, chainData.sourceItem);
                        entity.replaceChainData(chainData, null);
                        attachChain(entity, newChainData, null, true);
                        continue;
                    }
                }

                if (entity.age > 100) {
                    ConnectibleChains.LOGGER.info("Dropping chain connection as we have not been able to find chainholder for {}", chainData);
                    entity.dropItem(serverWorld, chainData.sourceItem);
                    entity.replaceChainData(chainData, null);
                }
            }
        }
    }

    private static <E extends BlockAttachedEntity & Chainable> void detachChain(E entity, ChainData chainData, boolean sendPacket, boolean dropItem) {
        if (chainData.chainHolder != null) {
            entity.replaceChainData(chainData, null);
            entity.onChainDetached(chainData);
            if (entity.getWorld() instanceof ServerWorld serverWorld) {
                // SERVER-SIDE //
                if (dropItem) {
                    entity.dropItem(serverWorld, chainData.sourceItem);
                }

                if (sendPacket) {
                    serverWorld.getChunkManager().sendToOtherNearbyPlayers(entity, new ChainAttachS2CPacket(entity, chainData.chainHolder, null, chainData.sourceItem).asPacket());
                }
                ChainCollisionEntity.destroyCollision(serverWorld, chainData);
            }
        }
    }

    private static <E extends BlockAttachedEntity & Chainable> void attachChain(E entity, ChainData chainData, @Nullable Entity previousHolder, boolean sendPacket) {
        if (chainData.chainHolder == null) {
            throw new IllegalArgumentException("Given");
        }

        entity.replaceChainData(entity.getChainData(previousHolder), chainData);
        entity.onChainAttached(chainData);

        if (sendPacket && entity.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().sendToOtherNearbyPlayers(entity, new ChainAttachS2CPacket(entity, previousHolder, chainData.chainHolder, chainData.sourceItem).asPacket());
            if (chainData.chainHolder instanceof Chainable) {
                ChainCollisionEntity.createCollision(entity, chainData);
            }
        }
    }

    static <E extends BlockAttachedEntity & Chainable> void tickChain(ServerWorld world, E entity) {
        // SERVER-SIDE //
        HashSet<ChainData> chainDataSet = entity.getChainDataSet();
        resolveChainDataSet(entity, chainDataSet);

        for (ChainData chainData : new HashSet<>(chainDataSet)) {
            Entity chainHolder = entity.getChainHolder(chainData);
            if (chainHolder != null) {
                if (!entity.isAlive() || !chainHolder.isAlive()) {
                    if (!entity.isAlive()) {
                        ConnectibleChains.LOGGER.info("Removing chain since chainReceiver ({}) is no longer alive, data: {}", entity, chainData);
                    } else {
                        ConnectibleChains.LOGGER.info("Removing chain since chainHolder ({}) is no longer alive, data: {}", chainHolder, chainData);
                    }

                    if (world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                        entity.detachChain(chainData);
                    } else {
                        entity.detachChainWithoutDrop(chainData);
                    }
                }

                // The holder might now be detached, so we refresh again.
                chainHolder = entity.getChainHolder(chainData);
                if (chainHolder != null && chainHolder.getWorld().equals(entity.getWorld())) {
                    float distanceTo = entity.distanceTo(chainHolder);
                    if (!entity.beforeChainTick(chainHolder, distanceTo)) {
                        continue;
                    }

                    if (distanceTo > getMaxChainLength()) {
                        entity.breakLongChain(chainData);
                    }
                }
            }
        }
    }

    @Nullable
    private static <E extends BlockAttachedEntity & Chainable> Entity getChainHolder(E entity, ChainData chainData) {
        if (!entity.getChainDataSet().contains(chainData)) {
            return null;
        }

        if (chainData.unresolvedChainHolderId != 0 && entity.getWorld().isClient) {
            // CLIENT-SIDE //
            Entity chainHolder = entity.getWorld().getEntityById(chainData.unresolvedChainHolderId);
            if (chainHolder instanceof Entity) {
                entity.replaceChainData(chainData, new ChainData(chainHolder, chainData.sourceItem));
            }
        }

        return chainData.chainHolder;
    }

    /**
     * Get the sound used for the source item, this way the sound is consistent.
     */
    static BlockSoundGroup getSourceBlockSoundGroup(Item sourceItem) {
        return switch (sourceItem) {
            case BlockItem blockItem -> blockItem.getBlock().getDefaultState().getSoundGroup();
            case LeadItem ignored -> new BlockSoundGroup(
                    1.0f,
                    1.0f,
                    SoundEvents.ENTITY_LEASH_KNOT_BREAK,
                    BlockSoundGroup.WOOL.getStepSound(),
                    SoundEvents.ENTITY_LEASH_KNOT_PLACE,
                    BlockSoundGroup.WOOL.getHitSound(),
                    BlockSoundGroup.WOOL.getFallSound()
            );
            case null, default -> BlockSoundGroup.CHAIN;
        };
    }

    default boolean canAttachTo(Entity entity) {
        return canAttachTo((BlockAttachedEntity & Chainable) this, entity);
    }

    HashSet<ChainData> getChainDataSet();

    /**
     * Alter the chain data set by replacing or adding or removing based on the given values.
     * If old is null, we assume you add new one.
     * <p>
     * If both are null, nothing should happen.
     *
     * @param oldChainData The old value, or null if you add a new one.
     * @param newChainData The new value, or null if you are removing the old.
     */
    void replaceChainData(@Nullable ChainData oldChainData, @Nullable ChainData newChainData);

    void setChainData(HashSet<ChainData> chainData);

    default void addUnresolvedChainHolderId(int unresolvedOldChainHolderId, int unresolvedNewChainHolderId, Item sourceItem) {
        ChainData oldChainData = null, newChainData = null;
        if (unresolvedOldChainHolderId != 0) {
            oldChainData = new ChainData(unresolvedOldChainHolderId, sourceItem);
        }
        if (unresolvedNewChainHolderId != 0) {
            newChainData = new ChainData(unresolvedNewChainHolderId, sourceItem);
        }

        this.replaceChainData(oldChainData, newChainData);
    }

    default void readChainDataFromNbt(NbtCompound nbt) {
        setSourceItem(Registries.ITEM.get(Identifier.tryParse(nbt.getString(SOURCE_ITEM_KEY))));

        HashSet<ChainData> chainData = readChainDataSet((BlockAttachedEntity & Chainable) this, nbt);
        if (!this.getChainDataSet().isEmpty() && chainData.isEmpty()) {
            this.detachAllChainsWithoutDrop();
        }

        this.setChainData(chainData);
    }

    default void writeChainDataSetToNbt(NbtCompound nbt, HashSet<ChainData> chainDataSet) {
        nbt.putString(SOURCE_ITEM_KEY, Registries.ITEM.getId(getSourceItem()).toString());

        NbtList linksTag = new NbtList();
        for (ChainData chainData : chainDataSet) {
            Either<UUID, BlockPos> either = chainData.unresolvedChainData;
            if (chainData.chainHolder instanceof ChainKnotEntity chainKnotEntity) {
                either = Either.right(chainKnotEntity.getAttachedBlockPos());
            } else if (chainData.chainHolder != null) {
                either = Either.left(chainData.chainHolder.getUuid());
            }

            if (either != null) {
                String sourceItem = Registries.ITEM.getId(chainData.sourceItem).toString();
                linksTag.add(either.map(uuid -> {
                    NbtCompound nbtCompound = new NbtCompound();
                    nbtCompound.putUuid("UUID", uuid);
                    nbtCompound.putString(SOURCE_ITEM_KEY, sourceItem);
                    return nbtCompound;
                }, blockPos -> {
                    NbtCompound nbtCompound = new NbtCompound();
                    nbtCompound.putInt("DestX", blockPos.getX());
                    nbtCompound.putInt("DestY", blockPos.getY());
                    nbtCompound.putInt("DestZ", blockPos.getZ());
                    nbtCompound.putString(SOURCE_ITEM_KEY, sourceItem);
                    return nbtCompound;
                }));
            }
        }
        if (!linksTag.isEmpty()) {
            nbt.put(CHAINS_NBT_KEY, linksTag);
        }
    }

    default void detachChain(ChainData chainData) {
        detachChain((BlockAttachedEntity & Chainable) this, chainData, true, true);
    }

    default void detachChainWithoutDrop(ChainData chainData) {
        detachChain((BlockAttachedEntity & Chainable) this, chainData, true, false);
    }

    default void detachAllChains() {
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            detachChain(chainData);
        }
    }

    default void detachAllChainsWithoutDrop() {
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            detachChainWithoutDrop(chainData);
        }
    }

    default void onChainDetached(ChainData removedChainData) {
    }

    /**
     * Called before the default chain-ticking logic.
     * Subclasses can override this to add their own logic to it.
     * <p>
     * {@return whether the default logic should run after this.}
     *
     * @see Chainable#tickChain
     */
    default boolean beforeChainTick(Entity chainHolder, float distance) {
        return true;
    }

    default void breakLongChain(ChainData chainData) {
        ConnectibleChains.LOGGER.info("Breaking chain as it is too long! {}", chainData);
        this.detachChain(chainData);
    }

    default void attachChain(ChainData chainData, @Nullable Entity previousHolder, boolean sendPacket) {
        attachChain((BlockAttachedEntity & Chainable) this, chainData, previousHolder, sendPacket);
    }

    default void onChainAttached(ChainData newChainData) {
    }

    @Nullable
    default Entity getChainHolder(ChainData chainData) {
        return getChainHolder((BlockAttachedEntity & Chainable) this, chainData);
    }

    @Nullable
    default ChainData getChainData(@Nullable Entity holder) {
        if (holder != null) {
            for (ChainData chainData : new HashSet<>(getChainDataSet())) {
                if (getChainHolder(chainData) == holder) {
                    return chainData;
                }
            }
        }
        return null;
    }

    /**
     * The item that used as the wrapping. Individual ChainData have their own source.
     */
    Item getSourceItem();

    void setSourceItem(Item item);

    default BlockSoundGroup getSourceBlockSoundGroup() {
        return getSourceBlockSoundGroup(getSourceItem());
    }

    Vec3d getChainPos(float delta);

    public static final class ChainData {

        /**
         * A list of collision entity ids, only used in the server.
         */
        public final IntArrayList collisionStorage = new IntArrayList(16);
        @Nullable
        public final Either<UUID, BlockPos> unresolvedChainData;
        @NotNull
        public final Item sourceItem;
        final int unresolvedChainHolderId;
        /**
         * The Holder is the entity that gets/receives the chain.
         * Either a ChainKnotEntity or a Player.
         */
        @Nullable
        private final Entity chainHolder;

        public ChainData(@Nullable Either<UUID, BlockPos> unresolvedChainData, @NotNull Item sourceItem) {
            this.unresolvedChainData = unresolvedChainData;
            this.sourceItem = sourceItem;
            this.chainHolder = null;
            this.unresolvedChainHolderId = 0;
        }

        public ChainData(@Nullable Entity chainHolder, @NotNull Item sourceItem) {
            this.chainHolder = chainHolder;
            this.sourceItem = sourceItem;
            this.unresolvedChainData = null;
            this.unresolvedChainHolderId = 0;
        }

        public ChainData(int unresolvedChainHolderId, @NotNull Item sourceItem) {
            this.unresolvedChainHolderId = unresolvedChainHolderId;
            this.sourceItem = sourceItem;
            this.chainHolder = null;
            this.unresolvedChainData = null;
        }

        public BlockSoundGroup getSourceBlockSoundGroup() {
            return Chainable.getSourceBlockSoundGroup(sourceItem);
        }

        private int getHolderId() {
            return chainHolder != null ? chainHolder.getId() : unresolvedChainHolderId;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChainData chainData)) {
                return false;
            }

            int thisId = getHolderId();
            int thatId = chainData.getHolderId();
            if (thisId != 0 && thisId == thatId) {
                return true;
            }
            return unresolvedChainData != null && unresolvedChainData.equals(chainData.unresolvedChainData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(getHolderId());
        }


        @Override
        public String toString() {
            return "ChainData{" +
                    "collisionStorage=" + collisionStorage +
                    ", unresolvedChainData=" + unresolvedChainData +
                    ", sourceItem=" + sourceItem +
                    ", unresolvedChainHolderId=" + unresolvedChainHolderId +
                    ", chainHolder=" + chainHolder +
                    '}';
        }
    }
}
