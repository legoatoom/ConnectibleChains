/*
 * Copyright (C) 2024 legoatoom.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.item.ChainItemCallbacks;
import com.github.legoatoom.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.github.legoatoom.connectiblechains.tag.ModTagRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has links to others of its kind, and is a combination of {@link MobEntity}
 * and {@link LeashKnotEntity}.
 *
 * @author legoatoom, Qendolin
 */
public class ChainKnotEntity extends AbstractDecorationEntity implements Chainable, ChainLinkEntity {

    private HashSet<ChainData> chainDataSet = new HashSet<>();

    @NotNull
    private Item sourceItem;

    protected ChainKnotEntity(EntityType<? extends AbstractDecorationEntity> entityType, World world) {
        super(entityType, world);
        sourceItem = Items.CHAIN; // Should be overwritten by spawn package.
    }

    public ChainKnotEntity(World world, BlockPos pos, @NotNull Item sourceItem) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        this.sourceItem = sourceItem;
        setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    @Nullable
    public static ChainKnotEntity getOrNull(World world, BlockPos pos) {
        List<ChainKnotEntity> chainKnotEntities = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).expand(1));
        for (ChainKnotEntity chainKnotEntity : chainKnotEntities) {
            if (chainKnotEntity.getDecorationBlockPos().equals(pos)) {
                return chainKnotEntity;
            }
        }

        return null;
    }

    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, @NotNull Item newSourceItem) {
        ChainKnotEntity chainKnotEntity = getOrNull(world, pos);

        if (chainKnotEntity == null) {
            chainKnotEntity = new ChainKnotEntity(world, pos, newSourceItem);
            world.spawnEntity(chainKnotEntity);
        }
        return chainKnotEntity;
    }

    @Override
    public HashSet<ChainData> getChainDataSet() {
        return chainDataSet;
    }

    @Override
    public void replaceChainData(@Nullable ChainData oldChainData, @Nullable ChainData newChainData) {
        if (oldChainData != null) {
            if (!chainDataSet.removeIf(chainData -> chainData.equals(oldChainData) || chainData.equals(newChainData))) {
                ConnectibleChains.LOGGER.warn("Attempted to remove {}, from {}. But it was not able to find it?", oldChainData, chainDataSet);
            }
        }

        if (newChainData != null) chainDataSet.add(newChainData);
    }

    @Override
    public void setChainData(HashSet<ChainData> chainDataSet) {
        this.chainDataSet = chainDataSet;
    }

    @Override
    public void tick() {
        if (!this.getWorld().isClient && !this.canStayAttached()) {
            this.discard();
            this.onBreak(null);
        }
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Chainable.tickChain(serverWorld, this);
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {

        ItemStack handStack = player.getStackInHand(hand);
        if (getWorld().isClient()) {
            // CLIENT-SIDE
            ChainData chainDataForPlayer = getChainData(player);
            if (chainDataForPlayer != null) {
                if (!player.isCreative()) {
                    player.giveItemStack(new ItemStack(chainDataForPlayer.sourceItem));
                }
                return ActionResult.SUCCESS;
            }

            // Client handle attach.
            if (handStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                if (handStack.getItem() != this.sourceItem) return ActionResult.PASS;

                if (!player.isCreative()) {
                    handStack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            // Client handle destroy.
            if (handStack.isIn(ConventionalItemTags.SHEARS)) {
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }
        // SERVER-SIDE
        if (this.isAlive() && player.getWorld() instanceof ServerWorld serverWorld) {
            // CASE: Attempt to attach to this Knot.
            boolean hasConnectedFromPlayer = false;
            List<Chainable> list = ChainItemCallbacks.collectChainablesAround(this.getWorld(), this.getDecorationBlockPos(), entity -> entity.getChainData(player) != null);

            for (Chainable chainable : list) {
                // TODO: Kinda inefficient, perhaps return a list of pairs? Chainable+ChainData.
                ChainData chainData = chainable.getChainData(player);
                if (chainData == null || !chainable.canAttachTo(this)) continue;

                if (chainData.sourceItem != this.sourceItem) continue;

                chainable.attachChain(new ChainData(this, chainData.sourceItem), player, true);
                hasConnectedFromPlayer = true;
            }

            if (hasConnectedFromPlayer) {
                onPlace();
                return ActionResult.SUCCESS;
            }

            // CASE: Player interacts with knot that they are currently attached to. Causing it to be removed.
            ChainData matchingData = null;
            for (ChainData chainData : new HashSet<>(getChainDataSet())) {
                if (player == getChainHolder(chainData)) {
                    matchingData = chainData;
                    break;
                }
            }
            if (matchingData != null) {
                detachChainWithoutDrop(matchingData);
                if (!player.isCreative()) {
                    player.giveItemStack(new ItemStack(matchingData.sourceItem));
                }
                this.emitGameEvent(GameEvent.ENTITY_INTERACT, player);

                return ActionResult.SUCCESS;
            }

            // CASE: Player interacts with knot that they are currently NOT attached to. Make a new connection.
            if (handStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                if (handStack.getItem() != this.sourceItem) return ActionResult.PASS;

                onPlace();
                attachChain(new ChainData(player, handStack.getItem()), null, true);
                if (!player.isCreative()) {
                    handStack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            // CASE: Interacted with anything else, check for shears
            if (handStack.isIn(ConventionalItemTags.SHEARS)) {
                ConnectibleChains.LOGGER.debug("Removing all connections due to player {} action on chain: {}", player, this);
                if (player.isCreative()) {
                    detachAllChainsWithoutDrop();
                } else {
                    detachAllChains();
                }
                this.remove(RemovalReason.DISCARDED);
                this.onBreak(player);

                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        if (!super.handleAttack(attacker)) playSound(getSourceBlockSoundGroup().getHitSound(), 0.5F, 1.0F);
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient) return false;
        ActionResult result = onDamageFrom(source, getSourceBlockSoundGroup().getHitSound());

        if (!result.isAccepted()) {
            return false;
        }

        if (result == ActionResult.SUCCESS) {
            ConnectibleChains.LOGGER.debug("Dropping all chains from knot ({}) due to receiving damage from source: {}", this, source);
            detachAllChains();
        }

        return super.damage(source, amount);
    }

    @Override
    protected void removeFromDimension() {
        super.removeFromDimension();
        detachAllChainsWithoutDrop();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        this.writeChainDataSetToNbt(nbt, this.chainDataSet);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        this.readChainDataFromNbt(nbt);
    }

    /**
     * @see LeashKnotEntity#updateAttachmentPosition
     */
    @Override
    protected void updateAttachmentPosition() {
        BlockPos attachedBlockPos = getDecorationBlockPos();
        setPos(attachedBlockPos.getX() + 0.5D, attachmentPos.getY() + 0.5D, attachedBlockPos.getZ() + 0.5D);
        double width = getType().getWidth() / 2.0;
        double height = getType().getHeight();
        setBoundingBox(new Box(getX() - width, getY(), getZ() - width, getX() + width, getY() + height, getZ() + width));
    }

    @Override
    public Box getVisibilityBoundingBox() {
        Box result = super.getVisibilityBoundingBox();
        for (ChainData chainData : new HashSet<>(this.getChainDataSet())) {
            Entity entity = this.getChainHolder(chainData);
            if (entity == null) continue;

            result = result.union(entity.getBoundingBox()); // Not visible otherwise it would also get its chains.
        }
        return result;
    }

    @Override
    public boolean canStayAttached() {
        return this.getWorld().getBlockState(this.getDecorationBlockPos()).isIn(ModTagRegistry.CHAIN_CONNECTIBLE);
    }

    @Override
    public void onChainAttached(ChainData newChainData) {
        this.playSound(newChainData.getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    @Override
    public @NotNull Item getSourceItem() {
        return sourceItem;
    }

    @Override
    public void setSourceItem(@NotNull Item sourceItem) {
        this.sourceItem = sourceItem;
    }

    @Override
    public void onChainDetached(ChainData removedChainData) {
        this.playSound(removedChainData.getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    public void onPlace() {
        this.playSound(getSourceBlockSoundGroup().getPlaceSound(), 1.0F, 1.0F);
    }

    @Override
    public void onBreak(@Nullable Entity breaker) {
        this.playSound(getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        for (ChainData chainData : getChainDataSet()) {
            ServerPlayNetworking.send(player, new ChainAttachS2CPacket(this, null, getChainHolder(chainData), chainData.sourceItem));
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        int id = Registries.ITEM.getRawId(getSourceItem());
        return new EntitySpawnS2CPacket(this, id, this.getDecorationBlockPos());
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        int rawChainItemSourceId = packet.getEntityData();
        this.sourceItem = Registries.ITEM.get(rawChainItemSourceId);
    }

    @Override
    public float applyRotation(BlockRotation rotation) {
        for (ChainData chainData : chainDataSet) {
            chainData.applyRotation(rotation);
        }
        return super.applyRotation(rotation);
    }

    @Override
    public @Nullable ItemStack getPickBlockStack() {
        return new ItemStack(getSourceItem());
    }

    public Vec3d getChainPos(float delta) {
        return this.getLerpedPos(delta).add(0.0, 0.2, 0.0);
    }

    @Override
    public int getWidthPixels() {
        return 9;
    }

    @Override
    public int getHeightPixels() {
        return 9;
    }
}