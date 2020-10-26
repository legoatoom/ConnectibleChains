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
        return register1((Block)item.getBlock(), (Item)item);
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
