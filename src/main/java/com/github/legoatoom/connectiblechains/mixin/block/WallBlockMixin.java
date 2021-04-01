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

package com.github.legoatoom.connectiblechains.mixin.block;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * This mixin is needed to allow a player to right click on a fence without having a chain in hand and still
 * try to make connections if it wants this fence to be the endpoint.
 *
 * @author legoatoom.
 */
@Mixin(WallBlock.class)
public class WallBlockMixin {

    /**
     * We create an onUse method here that overrides the onUse that {@link WallBlock} has inherited from {@link net.minecraft.block.AbstractBlock}.
     * Yes, that apparently works this way.
     */
    @SuppressWarnings("unused")
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            ItemStack itemStack = player.getStackInHand(hand);
            return itemStack.getItem() == Items.CHAIN ? ActionResult.SUCCESS : ActionResult.PASS;
        } else {
            return ChainKnotEntity.tryAttachHeldChainsToBlock(player, world, pos, ChainKnotEntity.getOrCreate(world, pos, true)) ? ActionResult.SUCCESS : ActionResult.PASS;
        }
    }
}
