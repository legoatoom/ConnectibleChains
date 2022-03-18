package com.github.legoatoom.connectiblechains.compat;

import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashSet;
import java.util.Set;

public class BuiltinCompat {
    /**
     * A list of item ids that this mod provides basic support for by default
     */
    public static final Set<Identifier> BUILTIN_TYPES = new HashSet<>() {{
        add(new Identifier("betterend:thallasium_chain"));
        add(new Identifier("betterend:terminite_chain"));
        add(new Identifier("betternether:cincinnasite_chain"));
        add(new Identifier("valley:golden_chain"));
        add(new Identifier("valley:netherite_chain"));
        add(new Identifier("valley:copper_chain"));
        add(new Identifier("valley:exposed_copper_chain"));
        add(new Identifier("valley:weathered_copper_chain"));
        add(new Identifier("valley:oxidized_copper_chain"));
        add(new Identifier("valley:waxed_copper_chain"));
        add(new Identifier("valley:waxed_exposed_copper_chain"));
        add(new Identifier("valley:waxed_weathered_copper_chain"));
        add(new Identifier("valley:waxed_oxidized_copper_chain"));
    }};

    private static final Set<Identifier> REGISTERED_BUILTIN_TYPES = new HashSet<>();

    /**
     * Registers all builtin types for currently registered items and
     * sets up a listener for furure item registrations.
     */
    public static void init() {
        RegistryEntryAddedCallback.event(Registry.ITEM).register((rawId, id, object) -> registerTypeForBuiltin(id));

        for (Identifier itemId : BUILTIN_TYPES) {
            registerTypeForBuiltin(itemId);
        }
    }

    /**
     * Checks if a builtin type exists for {@code itemId} and then registers a type for it once.
     *
     * @param itemId The id of an item
     */
    public static void registerTypeForBuiltin(Identifier itemId) {
        if (!BUILTIN_TYPES.contains(itemId) || REGISTERED_BUILTIN_TYPES.contains(itemId)) return;
        if (!Registry.ITEM.containsId(itemId)) return;
        ChainTypesRegistry.register(Registry.ITEM.get(itemId));
        REGISTERED_BUILTIN_TYPES.add(itemId);
    }
}
