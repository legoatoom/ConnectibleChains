package com.github.legoatoom.connectiblechains.compat;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.*;

public class BuiltinCompat {
    private static final Set<Identifier> BUILTIN_TYPES = new HashSet<>(){{
       add(new Identifier("betterend:thallasium_chain"));
       add(new Identifier("betterend:terminite_chain"));
       add(new Identifier("betternether:cincinnasite_chain"));
       add(new Identifier("valley:golden_chain"));
       add(new Identifier("valley:netherite_chain"));
       add(new Identifier("valley:copper_chain"));
    }};

    private static final Set<Identifier> REGISTERED_BUILTIN_TYPES = new HashSet<>();

    public static void init() {
        RegistryEntryAddedCallback.event(Registry.ITEM).register((rawId, id, object) -> {
            registerTypeForBuiltin(id);
        });

        for (Identifier itemId : BUILTIN_TYPES) {
            registerTypeForBuiltin(itemId);
        }
    }

    public static void registerTypeForBuiltin(Identifier itemId) {
        if(!BUILTIN_TYPES.contains(itemId) || REGISTERED_BUILTIN_TYPES.contains(itemId)) return;
        if(!Registry.ITEM.containsId(itemId)) return;
        ChainTypesRegistry.register(Registry.ITEM.get(itemId));
        REGISTERED_BUILTIN_TYPES.add(itemId);
    }
}
