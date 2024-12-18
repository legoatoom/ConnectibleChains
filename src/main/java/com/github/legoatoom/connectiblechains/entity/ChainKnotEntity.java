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

import com.github.legoatoom.connectiblechains.item.ChainItemInfo;
import com.github.legoatoom.connectiblechains.tag.ModTagRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has links to others of its kind, and is a combination of {@link net.minecraft.entity.mob.MobEntity}
 * and {@link net.minecraft.entity.decoration.LeashKnotEntity}.
 *
 * @author legoatoom, Qendolin
 */
public class ChainKnotEntity extends BlockAttachedEntity implements Chainable {

    private HashSet<ChainData> chainDataSet = new HashSet<>();

    /**
     * The item used as wrapping for this knot
     */
    private Item sourceItem = Items.CHAIN;

    protected ChainKnotEntity(EntityType<? extends BlockAttachedEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos, Item sourceItem) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        this.sourceItem = sourceItem;
        setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public static ChainKnotEntity getOrCreate(World world, BlockPos pos, Item sourceItem) {
        List<ChainKnotEntity> chainKnotEntities = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).expand(1));
        for (ChainKnotEntity chainKnotEntity : chainKnotEntities) {
            if (chainKnotEntity.getAttachedBlockPos().equals(pos)) {
                chainKnotEntity.sourceItem = sourceItem;
                return chainKnotEntity;
            }
        }

        ChainKnotEntity chainKnotEntity = new ChainKnotEntity(world, pos, sourceItem);
        world.spawnEntity(chainKnotEntity);
        return chainKnotEntity;
    }

    @Override
    public HashSet<ChainData> getChainDataSet() {
        return chainDataSet;
    }

    @Override
    public void replaceChainData(@Nullable ChainData oldChainData, @Nullable ChainData newChainData) {
        chainDataSet.remove(oldChainData);
        chainDataSet.add(newChainData);
    }

    @Override
    public void setChainData(HashSet<ChainData> chainDataSet) {
        this.chainDataSet = chainDataSet;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack handStack = player.getStackInHand(hand);
        if (getWorld().isClient()) {
            // CLIENT-SIDE
            if (handStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                this.sourceItem = handStack.getItem();
                handStack.decrementUnlessCreative(1, player);
                return ActionResult.SUCCESS;
            }

            if (ChainLinkEntity.canDestroyWith(handStack)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        // SERVER-SIDE
        if (this.isAlive()) {
            // CASE: Attempt to attach to this Knot.
            boolean hasConnectedFromPlayer = false;
            List<Chainable> list = ChainItemInfo.collectChainablesAround(this.getWorld(), this.getAttachedBlockPos(), chainable -> chainable.getChainData(player) != null || chainable.getChainData(this) != null);

            for (Chainable chainable : list) {
                // TODO: Kinda inefficient, perhaps return a list of pairs? Chainable+ChainData.
                ChainData chainData = chainable.getChainData(player);
                if (chainData == null) continue;
                chainData.setChainHolder(this);
                chainable.attachChain(chainData, player, true);
                hasConnectedFromPlayer = true;
            }

            if (hasConnectedFromPlayer) {
                onPlace();
                return ActionResult.CONSUME;
            }

            // CASE: Player interacts with knot that they are currently attached to. Causing it to be removed.
            ChainData matchingData = null;
            for (ChainData chainData : getChainDataSet()) {
                if (chainData.chainHolder == player) {
                    matchingData = chainData;
                    break;
                }
            }
            if (matchingData != null) {
                if (player.isInCreativeMode()) {
                    detachChainWithoutDrop(matchingData);
                } else {
                    detachChain(matchingData);
                }
                this.emitGameEvent(GameEvent.ENTITY_INTERACT, player);

                return ActionResult.SUCCESS.noIncrementStat();
            }

            // CASE: Player interacts with knot that they are currently NOT attached to. Make a new connection.
            if (handStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                this.sourceItem = handStack.getItem();
                attachChain(new ChainData(player, sourceItem), null, true);
                handStack.decrementUnlessCreative(1, player);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
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
        setPos(attachedBlockPos.getX() + 0.5D, attachedBlockPos.getY() + 0.5D, attachedBlockPos.getZ() + 0.5D);
        double width = getType().getWidth() / 2.0;
        double height = getType().getHeight();
        setBoundingBox(new Box(getX() - width, getY(), getZ() - width, getX() + width, getY() + height, getZ() + width));
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 1024.0; //TODO: Determine if this needs to be changed, it used to be just true.
    }

    @Override
    public boolean canStayAttached() {
        return false;
    }

    @Override
    public void onChainAttached(ChainData newChainData) {
        this.playSound(newChainData.getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    @Override
    public Item getSourceItem() {
        return sourceItem;
    }

    @Override
    public void onChainDetached(ChainData removedChainData) {
        Chainable.super.onChainDetached(removedChainData);
    }

    public void onPlace() {
        this.playSound(getSourceBlockSoundGroup().getPlaceSound(), 1.0F, 1.0F);
    }

    @Override
    public void onBreak(ServerWorld world, @Nullable Entity breaker) {
        this.playSound(getSourceBlockSoundGroup().getBreakSound(), 1.0F, 1.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }
}
