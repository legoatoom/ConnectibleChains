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

package com.github.legoatoom.connectiblechains.item;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.entity.ChainLinkEntity;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import com.github.legoatoom.connectiblechains.tag.ModTagRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.function.Predicate;

/**
 * Some static settings and functions for the chainItem.
 */
public class ChainItemInfo {


    /**
     * Because of how mods work, this function is called always when a player uses right click.
     * But if the right click doesn't involve this mod (No chain/block to connect to) then we ignore immediately.
     * <p>
     * If it does involve us, then we have work to do, we create connections remove items from inventory and such.
     *
     * @param player    PlayerEntity that right-clicked on a block.
     * @param world     The world the player is in.
     * @param hand      What hand the player used.
     * @param hitResult General information about the block that was clicked.
     * @return An ActionResult.
     */
    public static ActionResult chainUseEvent(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (player == null || player.isSneaking()) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);

        if (!ChainKnotEntity.canAttachTo(blockState)) {
            return ActionResult.PASS;
        } else if (world.isClient) {
            ItemStack handItem = player.getStackInHand(hand);
            if (handItem.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                return ActionResult.SUCCESS;
            }

            // Check if any held chains can be attached. This can be done without holding a chain item
            if (!ChainKnotEntity.getHeldChainsInRange(player, blockPos).isEmpty()) {
                return ActionResult.SUCCESS;
            }

            // Check if a knot exists and can be destroyed
            // Would work without this check but no swing animation would be played
            if (ChainKnotEntity.getKnotAt(player.getWorld(), blockPos) != null && ChainLinkEntity.canDestroyWith(stack)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }


        // 1. Try with existing knot, regardless of hand item
        ChainKnotEntity knot = ChainKnotEntity.getKnotAt(world, blockPos);
        if (knot != null) {
            if (knot.interact(player, hand) == ActionResult.CONSUME) {
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }

        // 2. Check if any held chains can be attached.
        List<ChainLink> attachableChains = ChainKnotEntity.getHeldChainsInRange(player, blockPos);

        // Use the held item as the new knot type
        Item knotType = stack.getItem();

        // Allow default interaction behaviour.
        if (attachableChains.isEmpty() && !stack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
            return ActionResult.PASS;
        }

        // Held item does not correspond to a type.
        if (!stack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
            knotType = attachableChains.getFirst().getSourceItem();
        }

        // 3. Create new knot if none exists and delegate interaction
        knot = new ChainKnotEntity(world, blockPos, knotType);
        knot.setGraceTicks((byte) 0);
        world.spawnEntity(knot);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return knot.interact(player, hand);
    }

    public static ActionResult attachHeldChainsToBlock(PlayerEntity player, World world, BlockPos pos, Item sourceItem) {
        ChainKnotEntity chainKnotEntity = null;
        List<Chainable> list = collectChainablesAround(world, pos, entity -> entity.getChainData(player) != null);

        for (Chainable chainable : list) {
            if (chainKnotEntity == null) {
                chainKnotEntity = ChainKnotEntity.getOrCreate(world, pos, sourceItem);
                chainKnotEntity.onPlace();
            }

            chainable.attachChain(new Chainable.ChainData(chainKnotEntity, sourceItem), player, true);
        }

        if (!list.isEmpty()) {
            world.emitGameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Emitter.of(player));
            return ActionResult.SUCCESS_SERVER;
        } else {
            return ActionResult.PASS;
        }
    }

    public static List<Chainable> collectChainablesAround(World world, BlockPos pos, Predicate<Chainable> predicate) {
        double distance = 7.0;

        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).expand(distance);
        return world.getEntitiesByClass(Entity.class, box, entity -> entity instanceof Chainable chainable && predicate.test(chainable)).stream().map(Chainable.class::cast).toList();
    }


    @Environment(EnvType.CLIENT)
    public static void infoToolTip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipType tooltipType, List<Text> texts) {
        if (ConnectibleChains.runtimeConfig.doShowToolTip()) {
            if (itemStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                if (Screen.hasShiftDown()) {
                    texts.add(1, Text.translatable("message.connectiblechains.connectible_chain_detailed").formatted(Formatting.AQUA));
                } else {
                    texts.add(1, Text.translatable("message.connectiblechains.connectible_chain").formatted(Formatting.YELLOW));
                }
            }
        }
    }
}
