package com.github.legoatoom.connectiblechains.mixin.server;


import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DynamicRegistryManager.class)
public abstract class DynamicRegistryManagerMixin {

    @Shadow
    private static <E> void addBuiltinEntries(DynamicRegistryManager.Impl manager, Registry<E> registry) {
    }

    @Shadow
    private static <E> void register(ImmutableMap.Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> infosBuilder, RegistryKey<? extends Registry<E>> registryRef, Codec<E> entryCodec, Codec<E> networkEntryCodec) {
    }

    @Inject(method = "method_30531",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/registry/DynamicRegistryManager;register(Lcom/google/common/collect/ImmutableMap$Builder;Lnet/minecraft/util/registry/RegistryKey;Lcom/mojang/serialization/Codec;)V",
                    ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private static void registerCustomDynamicRegistries(CallbackInfoReturnable<ImmutableMap<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>>> ci, ImmutableMap.Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> builder) {
//        register(builder, ConnectibleChainsRegistry.CHAIN_TYPE_KEY, DynChainType.CODEC, DynChainType.CODEC);
    }

    @Inject(at = @At("HEAD"), method = "copyFromBuiltin", cancellable = true)
    private static <R extends Registry<?>> void copyFromBuiltin(DynamicRegistryManager.Impl manager, RegistryKey<R> registryRef, CallbackInfo ci) {
//        if(registryRef == ConnectibleChainsRegistry.CHAIN_TYPE_KEY) {
//            addBuiltinEntries(manager, ConnectibleChainsRegistry.CHAIN_TYPE);
//            ci.cancel();
//        }
    }
}