package com.legoatoom.develop.items;

import net.minecraft.block.Block;
import net.minecraft.block.PlayerSkullBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

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
                ItemStack stack = playerEntity.getStackInHand(context.getHand());
                if (!(stack.hasTag() && stack.getOrCreateTag().contains("linkedPos"))){
                    CompoundTag linkedFromTag = NbtHelper.fromBlockPos(new BlockPos(context.getHitPos()));
                    linkedFromTag.putInt("dimension", world.getDimension().getType().getRawId());
                    stack.getOrCreateTag().put("linkedPos", linkedFromTag);
                } else {
                    CompoundTag linkToTag = stack.getOrCreateTag().getCompound("linkedPos");
                    BlockPos linkPos = NbtHelper.toBlockPos(linkToTag);
                    DimensionType dimensionType = DimensionType.byRawId(linkToTag.getInt("dimension"));
                    int distance = (int)Math.ceil(linkPos.getSquaredDistance(blockPos));
                    playerEntity.sendMessage(new TranslatableText("Distance is: " + distance), true);
                    int maxDistance = 69;
                    if (dimensionType != world.getDimension().getType()){
                        playerEntity.sendMessage(new TranslatableText("Wrong Dimension"), true);
                    } else if (linkPos.equals(blockPos)) {
                        playerEntity.sendMessage(new TranslatableText("Same Connection"), true);
                    } else if (distance > maxDistance){
                        playerEntity.sendMessage(new TranslatableText("Too far"), true);
                    } else {
                        // Finally, it can actually connect :D
                        playerEntity.sendMessage(new TranslatableText("Success"), true);
                    }
                }
            }
            return ActionResult.SUCCESS;
        } else {
            return ActionResult.PASS;
        }
    }
}
