package com.legoatoom.develop.items;

import com.legoatoom.develop.enitity.ChainKnotEntity;
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
        PlayerEntity playerEntity = context.getPlayer();
        if (!world.isClient && playerEntity != null) {
            ItemStack itemStack = playerEntity.getStackInHand(context.getHand());
            if (block.isIn(BlockTags.FENCES)) {
                if (playerEntity.isSneaking()) {
                    placeChain(context, world, blockPos, playerEntity, itemStack);
                } else {
                    ChainKnotEntity chainKnotEntity = ChainKnotEntity.getOrCreate(world, blockPos);
                    boolean bl = false;
                    double d = 7.0D;
                    int i = blockPos.getX();
                    int j = blockPos.getY();
                    int k = blockPos.getZ();
                    List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class,
                            new Box((double)i - d, (double)j - d, (double)k - d,
                                    (double)i + d, (double)j + d, (double)k + d));

                    for (ChainKnotEntity chainKnotEntity1 : list) {
                        if (chainKnotEntity1.getHoldingEntities().contains(playerEntity)
                                && !chainKnotEntity1.getHoldingEntities().contains(chainKnotEntity)
                                && !chainKnotEntity.getHoldingEntities().contains(chainKnotEntity1)) {
                            chainKnotEntity1.attachChain(chainKnotEntity, true, playerEntity.getEntityId());
                            chainKnotEntity1.detachChain(false,false, playerEntity);
                            bl = true;
                        }
                    }

                    if (!bl) {
                        chainKnotEntity.attachChain(playerEntity, true, 0);
                        itemStack.decrement(1);
                    }
                }
            } else {
                placeChain(context, world, blockPos, playerEntity, itemStack);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private void placeChain(ItemUsageContext context, World world, BlockPos blockPos,
                            PlayerEntity playerEntity, ItemStack itemStack) {
        BlockPos placePos = blockPos.offset(context.getSide());
        if (world.isAir(placePos)){
            world.setBlockState(placePos, Blocks.CHAIN.getDefaultState());
            if (playerEntity instanceof ServerPlayerEntity) {
                Criteria.PLACED_BLOCK.trigger((ServerPlayerEntity)playerEntity, blockPos, itemStack);
            }
            if (!playerEntity.isCreative()){
                itemStack.decrement(1);
            }
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
            world.playSound(null, blockPos, SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static ActionResult attachHeldChainsToBlock(PlayerEntity playerEntity, World world, BlockPos blockPos) {
        ChainKnotEntity chainKnotEntity = ChainKnotEntity.getOrCreate(world, blockPos);
        boolean bl = false;
        double d = 7.0D;
        int i = blockPos.getX();
        int j = blockPos.getY();
        int k = blockPos.getZ();
        List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class,
                new Box((double)i - d, (double)j - d, (double)k - d,
                        (double)i + d, (double)j + d, (double)k + d));

        for (ChainKnotEntity chainKnotEntity1 : list) {
            if (chainKnotEntity1.getHoldingEntities().contains(playerEntity)) {
                chainKnotEntity1.attachChain(chainKnotEntity, true, playerEntity.getEntityId());
                chainKnotEntity1.detachChain(false,false, playerEntity);
                bl = true;
            }
        }

        return bl ? ActionResult.SUCCESS : ActionResult.PASS;
    }
}

