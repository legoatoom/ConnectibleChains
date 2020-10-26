/*
 *     Copyright (C) 2020 legoatoom
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.items;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class ChainItem extends BlockItem {

    public ChainItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();
        PlayerEntity playerEntity = context.getPlayer();
        if (block.isIn(BlockTags.FENCES) && playerEntity != null && !playerEntity.isSneaking()) {
            if (!world.isClient) {
                if (!attachHeldMobsToBlock(playerEntity, world, blockPos).isAccepted()){
                    // Create new ChainKnot
                    ChainKnotEntity knot = ChainKnotEntity.getOrCreate(world, blockPos);
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


    public static ActionResult attachHeldMobsToBlock(PlayerEntity playerEntity, World world, BlockPos blockPos) {
        ChainKnotEntity leashKnotEntity = null;
        boolean bl = false;
        double d = ChainKnotEntity.MAX_RANGE;
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box((double)i - d, (double)j - d, (double)k - d, (double)i + d, (double)j + d, (double)k + d));

        for (ChainKnotEntity mobEntity : list) {
            if (mobEntity.getHoldingEntities().contains(playerEntity)) {
                if (leashKnotEntity == null) {
                    leashKnotEntity = ChainKnotEntity.getOrCreate(world, blockPos);
                }

                if (!mobEntity.equals(leashKnotEntity)) {
                    mobEntity.attachChain(leashKnotEntity, true, playerEntity.getEntityId());
                    bl = true;
                }
            }
        }

        return bl ? ActionResult.SUCCESS : ActionResult.PASS;
    }

}

