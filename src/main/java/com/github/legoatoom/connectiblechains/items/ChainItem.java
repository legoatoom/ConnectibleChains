package com.github.legoatoom.connectiblechains.items;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Iterator;
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

