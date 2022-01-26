package com.github.legoatoom.connectiblechains.mixin.server;

import com.github.legoatoom.connectiblechains.datafixer.ChainKnotFixer;
import com.mojang.datafixers.DataFixer;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtHelper.class)
public abstract class NbtHelperMixin {
    @Inject(at = @At("RETURN"), method = "update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/datafixer/DataFixTypes;Lnet/minecraft/nbt/NbtCompound;II)Lnet/minecraft/nbt/NbtCompound;", cancellable = true)
    private static void updateDataWithFixers(DataFixer fixer, DataFixTypes fixTypes, NbtCompound compound, int oldVersion, int targetVersion, CallbackInfoReturnable<NbtCompound> cir) {
        NbtCompound original = cir.getReturnValue(); // We do our fixes after vanilla.
        if (fixTypes == DataFixTypes.ENTITY_CHUNK) {
            NbtCompound finalTag = ChainKnotFixer.INSTANCE.update(original);
            cir.setReturnValue(finalTag);
        }
    }
}
