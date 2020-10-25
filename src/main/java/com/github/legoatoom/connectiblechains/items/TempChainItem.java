package com.github.legoatoom.connectiblechains.items;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
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

public class TempChainItem extends Item {

    public TempChainItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();
        if (block.isIn(BlockTags.FENCES)) {
            PlayerEntity playerEntity = context.getPlayer();
            if (!world.isClient && playerEntity != null) {
                if (!attachHeldMobsToBlock(playerEntity, world, blockPos).isAccepted()){
                    // Create new ChainKnot
                    ChainKnotEntity knot = ChainKnotEntity.getOrCreateWithoutConnection(world, blockPos);
                    knot.attachChain(playerEntity, true);

                    if(!playerEntity.isCreative())
                        context.getStack().decrement(1);
                }
            }
            return ActionResult.success(world.isClient);
        } else {
            return ActionResult.PASS;
        }
    }


    public static ActionResult attachHeldMobsToBlock(PlayerEntity playerEntity, World world, BlockPos blockPos) {
        ChainKnotEntity leashKnotEntity = null;
        boolean bl = false;
        double d = 7.0D;
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class, new Box((double)i - 7.0D, (double)j - 7.0D, (double)k - 7.0D, (double)i + 7.0D, (double)j + 7.0D, (double)k + 7.0D));

        for (ChainKnotEntity mobEntity : list) {
            if (mobEntity.getHoldingEntity() == playerEntity) {
                if (leashKnotEntity == null) {
                    leashKnotEntity = ChainKnotEntity.getOrCreate(world, blockPos);
                }

                mobEntity.attachChain(leashKnotEntity, true);
                bl = true;
            }
        }

        return bl ? ActionResult.SUCCESS : ActionResult.PASS;
    }

}

