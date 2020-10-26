package com.github.legoatoom.connectiblechains.mixin.block;

import com.github.legoatoom.connectiblechains.items.ChainItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LeadItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FenceBlock.class)
public abstract class FenceBlockMixin {

    @Inject(
            method = "onUse",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir){
        if (world.isClient()){
            ActionResult result = player.getStackInHand(hand).getItem().equals(Items.CHAIN) ? ActionResult.SUCCESS : cir.getReturnValue();
            cir.setReturnValue(result);
        } else {
            ActionResult result = ChainItem.attachHeldMobsToBlock(player, world, pos);
            if (result.isAccepted()){
                cir.setReturnValue(result);
            }
        }
    }
}
