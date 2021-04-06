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

package com.github.legoatoom.connectiblechains.item;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * This item was made since the original chain did not have its own class.
 *
 * @author legoatoom
 */
public class ChainItem extends BlockItem {

    public ChainItem(Block block, Settings settings) {
        super(block, settings);
    }

    /**
     * When used on a block that is allowed to create a chain on, we get the chainKnot or make one and
     * either connect it to the player or, if the player has already done this, connect the other chainKnot to it.
     *
     * @param context the context of the usage.
     * @return ActionResult
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();
        PlayerEntity playerEntity = context.getPlayer();
        if (ChainKnotEntity.canConnectTo(block) && playerEntity != null && !playerEntity.isSneaking()) {
            if (!world.isClient) {
                ChainKnotEntity knot = ChainKnotEntity.getOrCreate(world, blockPos, false);
                if (!ChainKnotEntity.tryAttachHeldChainsToBlock(playerEntity, world, blockPos, knot)){
                    // If this didn't work connect the player to the new chain instead.
                    assert knot != null; // This can never happen as long as getOrCreate has false as parameter.
                    if (knot.getHoldingEntities().contains(playerEntity)){
                        knot.detachChain(playerEntity, true, false);
                        knot.onBreak(null);
                        if(!playerEntity.isCreative())
                            context.getStack().increment(1);
                    } else {
                        knot.attachChain(playerEntity, true, 0);
                        knot.onPlace();
                        if (!playerEntity.isCreative())
                            context.getStack().decrement(1);
                    }
                }
            }
            return ActionResult.success(world.isClient);
        } else {
            return super.useOnBlock(context);
        }
    }


}

