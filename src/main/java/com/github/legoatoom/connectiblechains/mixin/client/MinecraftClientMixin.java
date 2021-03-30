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

package com.github.legoatoom.connectiblechains.mixin.client;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public HitResult crosshairTarget;

    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Inject(
            method = "doItemPick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/hit/EntityHitResult;getEntity()Lnet/minecraft/entity/Entity;",
                    shift = At.Shift.AFTER),
            slice = @Slice(
                    from = @At("HEAD"),
                    to = @At(value = "INVOKE",
                            target = "Lnet/minecraft/item/SpawnEggItem;forEntity(Lnet/minecraft/entity/EntityType;)Lnet/minecraft/item/SpawnEggItem;")
            ),
            cancellable = true
    )
    private void givePlayerChain(CallbackInfo ci){
        // This only happens if the player is creative, since entities cannot be pickItem if not in creative.
        if (this.crosshairTarget != null && this.interactionManager != null && this.player != null) {
            Entity entity = ((EntityHitResult) this.crosshairTarget).getEntity();
            if (entity instanceof ChainKnotEntity){
                ItemStack itemStack12 = new ItemStack(Items.CHAIN);
                PlayerInventory playerInventory = this.player.inventory;
                playerInventory.addPickBlock(itemStack12);
                this.interactionManager.clickCreativeStack(this.player.getStackInHand(Hand.MAIN_HAND), 36 + playerInventory.selectedSlot);
                ci.cancel();
            }
        }
    }

}
