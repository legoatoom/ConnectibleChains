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
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import com.github.legoatoom.connectiblechains.tag.ModTagRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
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
public class ChainItemCallbacks {


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

        if (blockState.isIn(ModTagRegistry.CHAIN_CONNECTIBLE)) {
            if (hasAnyLeadsToConnect(world, blockPos, player)) {
                return ActionResult.PASS;
            }
            if (stack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
                if (world instanceof ServerWorld serverWorld) {
                    ChainKnotEntity knot = ChainKnotEntity.getOrCreate(serverWorld, blockPos, stack.getItem());

                    return knot.interact(player, hand);
                }
                return ActionResult.SUCCESS;
            }
            if (world instanceof ServerWorld serverWorld) {
                // SERVER-SIDE //
                return attachHeldChainsToBlock(player, serverWorld, blockPos);
            }
            // CLIENT-SIDE //
            if (!collectChainablesAround(world, blockPos, entity -> entity.getChainData(player) != null).isEmpty()) {
                return ActionResult.SUCCESS;
            } else {
                return ActionResult.PASS;
            }
        }
        return ActionResult.PASS;
    }

    public static ActionResult attachHeldChainsToBlock(PlayerEntity player, ServerWorld world, BlockPos pos) {
        List<Chainable> list = collectChainablesAround(world, pos, entity -> entity.getChainData(player) != null);

        ChainKnotEntity chainKnotEntity = null;
        for (Chainable chainable : list) {
            if (chainKnotEntity == null) {
                chainKnotEntity = ChainKnotEntity.getOrCreate(world, pos, chainable.getSourceItem());
                chainKnotEntity.onPlace();
            }

            if (chainable.canAttachTo(chainKnotEntity)) {
                Chainable.ChainData chainData = chainable.getChainData(player);
                assert chainData != null;
                chainable.attachChain(new Chainable.ChainData(chainKnotEntity, chainData.sourceItem), player, true);
            }
        }

        if (!list.isEmpty()) {
            world.emitGameEvent(GameEvent.BLOCK_ATTACH, pos, GameEvent.Emitter.of(player));
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.PASS;
        }
    }

    public static List<Chainable> collectChainablesAround(World world, BlockPos pos, Predicate<Chainable> predicate) {
        double distance = ConnectibleChains.runtimeConfig.getMaxChainRange();

        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()).expand(distance);
        return world.getEntitiesByClass(Entity.class, box, entity -> entity instanceof Chainable chainable && predicate.test(chainable)).stream().map(Chainable.class::cast).toList();
    }

    /**
     * Backport helper.
     */
    public static boolean hasAnyLeadsToConnect(World world, BlockPos pos, PlayerEntity player) {
        LeashKnotEntity leashKnotEntity = null;
        boolean found = false;

        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        for (MobEntity mobEntity : world.getNonSpectatingEntities(MobEntity.class, new Box(i - 7.0, j - 7.0, k - 7.0, i + 7.0, j + 7.0, k + 7.0))) {
            if (mobEntity.getHoldingEntity() == player) {
                if (leashKnotEntity == null) {
                    leashKnotEntity = LeashKnotEntity.getOrCreate(world, pos);
                    leashKnotEntity.onPlace();
                }

                mobEntity.attachLeash(leashKnotEntity, true);
                found = true;
            }
        }
        return found;
    }


    @Environment(EnvType.CLIENT)
    public static void infoToolTip(ItemStack itemStack, TooltipContext ignoredTooltipContext, List<Text> texts) {
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
