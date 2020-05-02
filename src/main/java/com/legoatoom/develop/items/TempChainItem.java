package com.legoatoom.develop.items;

import com.legoatoom.develop.ConnectibleChains;
import com.legoatoom.develop.enitity.ChainKnotEntity;
import com.sun.net.httpserver.Filter;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class TempChainItem extends Item {

    public TempChainItem(Settings settings) {
        super(settings);
    }

    private final String linkName = ConnectibleChains.MODID + ":linkedPos";
    private final String dimensionName = ConnectibleChains.MODID + ":dimension";

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();
        PlayerEntity playerEntity = context.getPlayer();
        if (!world.isClient && playerEntity != null) {
            ItemStack stack = playerEntity.getStackInHand(context.getHand());
            if (block.isIn(BlockTags.FENCES)) {
                if (playerEntity.isSneaking()) {
                    if (stack.hasTag() && stack.getOrCreateTag().contains(linkName)) {
                        stack.removeSubTag(linkName);
                    } else {
                        placeChain(context, world, blockPos, playerEntity, stack);
                    }
                } else {
                    if (!(stack.hasTag() && stack.getOrCreateTag().contains(linkName))) {
                        ChainKnotEntity chainKnotEntity = ChainKnotEntity.getOrCreate(world, blockPos, false);
                        chainKnotEntity.attachChain(playerEntity, playerEntity);
                        CompoundTag linkedFromTag = NbtHelper.fromBlockPos(blockPos);
                        linkedFromTag.putInt(dimensionName, world.getDimension().getType().getRawId());
                        stack.getOrCreateTag().put(linkName, linkedFromTag);
                    } else {
                        CompoundTag linkToTag = stack.getOrCreateTag().getCompound(linkName);
                        BlockPos linkPos = NbtHelper.toBlockPos(linkToTag);
                        ChainKnotEntity chainKnotFrom = ChainKnotEntity.getOrCreate(world, linkPos, true);
                        DimensionType dimensionType = DimensionType.byRawId(linkToTag.getInt(dimensionName));
                        int distance = (int) Math.ceil(Math.sqrt(getSquaredDistance(blockPos, linkPos)));
                        int maxDistance = 7;
                        if (chainKnotFrom == null){
                            playerEntity.sendMessage(new TranslatableText("Original knot is missing"), true);
                        } else if (dimensionType != world.getDimension().getType()) {
                            playerEntity.sendMessage(new TranslatableText("Wrong dimension"), true);
                        } else if (linkPos.equals(blockPos)) {
                            playerEntity.sendMessage(new TranslatableText("Same connection"), true);
                        } else if (distance > maxDistance) {
                            playerEntity.sendMessage(new TranslatableText("Too far"), true);
                        } else if (stack.getCount() < distance && !playerEntity.isCreative()) {
                            playerEntity.sendMessage(new TranslatableText("Insufficient materials"), true);
                        } else {
                            // Finally, it can actually connect :D
                            ChainKnotEntity chainKnotTo = ChainKnotEntity.getOrCreate(world, blockPos, false);
                            if (chainKnotTo != null) {
                                chainKnotTo.attachChain(chainKnotFrom, playerEntity);
                            }
                            playerEntity.sendMessage(new TranslatableText("Success"), true);
                            if (!playerEntity.isCreative()) {
                                stack.decrement(distance);
                            } stack.removeSubTag(linkName);
                        }
                    }
                }
            } else {
                placeChain(context, world, blockPos, playerEntity, stack);
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

    private double getSquaredDistance(BlockPos a, BlockPos b){
        double ax = (double)a.getX(); double ay = (double)a.getY(); double az = (double)a.getZ();
        double bx = (double)b.getX(); double by = (double)b.getY(); double bz = (double)b.getZ();
        double e = ax - bx;
        double f = ay - by;
        double g = az - bz;
        return e * e + f * f + g * g;
    }
}
