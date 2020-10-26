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

package com.github.legoatoom.connectiblechains.mixin.item;

import com.github.legoatoom.connectiblechains.items.ChainItem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Items.class)
public abstract class ItemsMixin {

    private static Item register1(BlockItem item) {
        return register1(item.getBlock(), item);
    }

    private static Item register1(Block block, Item item) {
        return register1(Registry.BLOCK.getId(block), item);
    }

    private static Item register1(Identifier id, Item item) {
        if (item instanceof BlockItem) {
            ((BlockItem)item).appendBlocks(Item.BLOCK_ITEMS, item);
        }

        return Registry.register(Registry.ITEM, id, item);
    }

    @Inject(
            method = "register(Lnet/minecraft/block/Block;Lnet/minecraft/item/ItemGroup;)Lnet/minecraft/item/Item;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void changeChainBlockItem(Block block, ItemGroup group, CallbackInfoReturnable<Item> cir){
        if (block == Blocks.CHAIN){
            cir.setReturnValue(ItemsMixin.register1(new ChainItem(Blocks.CHAIN, new Item.Settings().group(group))));
        }
    }
}
