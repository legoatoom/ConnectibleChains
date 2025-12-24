/*
 * Copyright (C) 2025 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.entity;


import net.minecraft.block.Block;
import net.minecraft.block.Oxidizable;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UseRemainderComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityOxidationHandler<E extends Entity & ChainLinkEntity> {
    private static final long IGNORE_WEATHERING_TICK = -2L;
    private static final long UNSET_WEATHERING_TICK = -1L;
    private static final int WEATHERING_TICK_FROM = 50400;
    private static final int WEATHERING_TICK_TO = 55200;

    private final E entity;
    private long nextOxidationAge = UNSET_WEATHERING_TICK;

    @Nullable
    private UUID lastStruckLightning;

    public EntityOxidationHandler(E entity) {
        this.entity = entity;
        if (getDegradationLevel().isEmpty()) {
            // Waxed
            this.nextOxidationAge = IGNORE_WEATHERING_TICK;
        }
    }

    public Optional<Oxidizable.OxidationLevel> getDegradationLevel() {
        if (getBlock() instanceof Oxidizable oxidizable) {
            return Optional.ofNullable(oxidizable.getDegradationLevel());
        } else {
            return Optional.empty();
        }
    }

    private Item getSourceItem() {
        return entity.getSourceItem();
    }

    private void setSourceItem(Item sourceItem) {
        entity.setSourceItem(sourceItem);
    }

    public Block getBlock() {
        return ((BlockItem) getSourceItem()).getBlock();
    }

    public void writeCustomData(WriteView view) {
        view.putLong("next_weather_age", this.nextOxidationAge);
    }

    public void readCustomData(ReadView view) {
        this.nextOxidationAge = view.getLong("next_weather_age", UNSET_WEATHERING_TICK);
    }

    private void decreaseOxidation() {
        Optional<Block> optionalBlock = Oxidizable.getDecreasedOxidationBlock(getBlock());
        if (optionalBlock.isEmpty()) {
            return;
        }

        setSourceItem(optionalBlock.get().asItem());
    }

    private void increaseOxidation() {
        Optional<Block> optionalBlock = Oxidizable.getIncreasedOxidationBlock(getBlock());
        if (optionalBlock.isEmpty()) {
            return;
        }

        setSourceItem(optionalBlock.get().asItem());
    }

    private void setIgnoreWeathering(boolean waxed) {
        Optional<Block> optionalBlock;
        if (waxed) {
            optionalBlock = Optional.ofNullable(HoneycombItem.UNWAXED_TO_WAXED_BLOCKS.get().get(getBlock()));
        } else {
            optionalBlock = Optional.ofNullable(HoneycombItem.WAXED_TO_UNWAXED_BLOCKS.get().get(getBlock()));
        }
        if (optionalBlock.isEmpty()) {
            return;
        }

        setSourceItem(optionalBlock.get().asItem());
    }

    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        World world = this.entity.getEntityWorld();

        if (itemStack.isOf(Items.HONEYCOMB) && this.nextOxidationAge != IGNORE_WEATHERING_TICK) {
            // Wax it
            world.syncWorldEvent(entity, WorldEvents.BLOCK_WAXED, entity.getBlockPos(), 0);
            this.nextOxidationAge = IGNORE_WEATHERING_TICK;
            setIgnoreWeathering(true);

            this.consume(player, hand, itemStack);
            return ActionResult.SUCCESS_SERVER;
        } else if (itemStack.isIn(ItemTags.AXES)) {
            if (this.nextOxidationAge == IGNORE_WEATHERING_TICK) {
                // Unwax it
                world.playSound(null, entity.getBlockPos(), SoundEvents.ITEM_AXE_SCRAPE, entity.getSoundCategory(), 1.0F, 1.0F);
                world.syncWorldEvent(entity, WorldEvents.WAX_REMOVED, entity.getBlockPos(), 0);
                this.nextOxidationAge = UNSET_WEATHERING_TICK;
                setIgnoreWeathering(false);

                itemStack.damage(1, player, hand.getEquipmentSlot());
                return ActionResult.SUCCESS_SERVER;
            } else {
                // Scrape it
                Optional<Oxidizable.OxidationLevel> oxidationLevel = this.getDegradationLevel();
                if (oxidationLevel.isPresent() && oxidationLevel.get() != Oxidizable.OxidationLevel.UNAFFECTED) {
                    world.playSound(null, entity.getBlockPos(), SoundEvents.ITEM_AXE_SCRAPE, entity.getSoundCategory(), 1.0F, 1.0F);
                    world.syncWorldEvent(entity, WorldEvents.BLOCK_SCRAPED, entity.getBlockPos(), 0);
                    this.nextOxidationAge = UNSET_WEATHERING_TICK;
                    decreaseOxidation();
                    itemStack.damage(1, player, hand.getEquipmentSlot());
                    return ActionResult.SUCCESS_SERVER;
                }
            }
        }
        return null;
    }

    private void consume(PlayerEntity player, Hand hand, ItemStack stack) {
        int i = stack.getCount();
        UseRemainderComponent useRemainderComponent = stack.get(DataComponentTypes.USE_REMAINDER);
        stack.decrementUnlessCreative(1, player);
        if (useRemainderComponent != null) {
            ItemStack itemStack = useRemainderComponent.convert(stack, i, player.isInCreativeMode(), player::giveOrDropStack);
            player.setStackInHand(hand, itemStack);
        }
    }

    public void updateWeathering(final Random random, final long gameTime) {
        if (this.nextOxidationAge == IGNORE_WEATHERING_TICK) {
            return;
        }

        if (this.nextOxidationAge == UNSET_WEATHERING_TICK) {
            this.nextOxidationAge = gameTime + random.nextBetween(WEATHERING_TICK_FROM, WEATHERING_TICK_TO);
            return;
        }

        Optional<Oxidizable.OxidationLevel> oxidationLevel = getDegradationLevel();
        if (oxidationLevel.isEmpty()) {
            // Should never happen as it would mean it is waxed.
            return;
        }

        boolean isFullyOxidized = oxidationLevel.get().equals(Oxidizable.OxidationLevel.OXIDIZED);
        if (gameTime >= this.nextOxidationAge && !isFullyOxidized) {
            increaseOxidation();
            boolean isNewStateFullyOxidized = oxidationLevel.get().getIncreased().equals(Oxidizable.OxidationLevel.OXIDIZED);
            this.nextOxidationAge = isNewStateFullyOxidized ? 0L : this.nextOxidationAge + random.nextBetween(WEATHERING_TICK_FROM, WEATHERING_TICK_TO);
        }
    }

    public void onStruckByLightning(LightningEntity lightning) {
        UUID uUID = lightning.getUuid();
        if (!uUID.equals(this.lastStruckLightning)) {
            this.lastStruckLightning = uUID;
            this.getDegradationLevel().ifPresent(oxidationLevel -> {
                if (oxidationLevel != Oxidizable.OxidationLevel.UNAFFECTED) {
                    this.nextOxidationAge = UNSET_WEATHERING_TICK;
                    decreaseOxidation();
                }
            });
        }
    }
}
