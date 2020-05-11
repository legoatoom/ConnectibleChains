package com.github.legoatoom.connectiblechains.mixin.block;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.items.TempChainItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
public class FenceBlockMixin {

    @Inject(
            method = "onUse(Lnet/minecraft/block/BlockState;" +
                    "Lnet/minecraft/world/World;" +
                    "Lnet/minecraft/util/math/BlockPos;" +
                    "Lnet/minecraft/entity/player/PlayerEntity;" +
                    "Lnet/minecraft/util/Hand;" +
                    "Lnet/minecraft/util/hit/BlockHitResult;" +
                    ")Lnet/minecraft/util/ActionResult;",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit,
                      CallbackInfoReturnable<ActionResult> callbackInfoReturnable) {
//        if (world.isClient) {
//            ItemStack itemStack = player.getStackInHand(hand);
//            if (itemStack.getItem() == ConnectibleChains.TEMP_CHAIN)
//                callbackInfoReturnable.setReturnValue(ActionResult.SUCCESS);
//        } else {
//            ActionResult result = TempChainItem.attachHeldChainsToBlock(player, world, pos);
//            if (result.isAccepted()) {
//                callbackInfoReturnable.setReturnValue(result);
//            }
//        }
    }
}
