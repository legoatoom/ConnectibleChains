package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class ChainTypesRegistry {
    public static final Identifier DEFAULT_CHAIN_TYPE_ID = Helper.identifier("iron_chain");
    public static final RegistryKey<? extends Registry<ChainType>> CHAIN_TYPE_KEY =
            RegistryKey.ofRegistry(Helper.identifier("chain_types"));
    public static final DefaultedRegistry<ChainType> REGISTRY = FabricRegistryBuilder.from(
                    new DefaultedRegistry<>(DEFAULT_CHAIN_TYPE_ID.toString(), CHAIN_TYPE_KEY, Lifecycle.stable(), null))
            .attribute(RegistryAttribute.SYNCED).buildAndRegister();

    public static final Map<Item, ChainType> ITEM_CHAIN_TYPES = new Object2ObjectOpenHashMap<>(64);
    public static final ChainType DEFAULT_CHAIN_TYPE;
    @SuppressWarnings("unused")
    public static final ChainType IRON_CHAIN;
    // Like SimpleRegistry#frozen, prevents further modification
    private static boolean frozen = false;

    static {
        // ITEM_CHAIN_TYPES has to be initialized before 'register' is called.
        // And IntelliJ just insisted on breaking it.

        DEFAULT_CHAIN_TYPE = register(DEFAULT_CHAIN_TYPE_ID, Items.CHAIN);
        IRON_CHAIN = DEFAULT_CHAIN_TYPE;
    }

    /**
     * Prevents registration with {@link #registerDynamic(Item)}
     * Registry will be locked once the game has loaded.
     */
    public static void lock() {
        frozen = true;
    }

    /**
     * Used to register chain types on initialization. Cannot register a type twice.
     *
     * @param item the chain type's item
     * @return the new {@link ChainType}
     */
    @SuppressWarnings("UnusedReturnValue")
    public static ChainType register(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        if (id == Registry.ITEM.getDefaultId()) {
            ConnectibleChains.LOGGER.error("Cannot create chain type with unregistered item: {}", item.getName());
            return DEFAULT_CHAIN_TYPE;
        }
        if (REGISTRY.containsId(id)) return REGISTRY.get(id);
        ChainType chainType = Registry.register(REGISTRY, id, new ChainType(item));
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }

    /**
     * Used to register chain types after initialization.
     * Can replace existing chain types.
     *
     * @param item the chain type's item
     */
    public static void registerDynamic(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        if (frozen) {
            ConnectibleChains.LOGGER.error("Tried to add {} but registry is locked.", id);
        }
        Optional<ChainType> existing = REGISTRY.getOrEmpty(id);
        OptionalInt rawId = OptionalInt.empty();
        if (existing.isPresent()) {
            if (existing.get().item() == item) return;
            rawId = OptionalInt.of(REGISTRY.getRawId(existing.get()));
        }

        RegistryEntry<ChainType> chainType = REGISTRY.replace(rawId, RegistryKey.of(REGISTRY.getKey(), id), new ChainType(item), Lifecycle.stable());
        ITEM_CHAIN_TYPES.put(item, chainType.value());
    }

    private static ChainType register(Identifier id, Item item) {
        ChainType chainType = Registry.register(REGISTRY, id, new ChainType(item));
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }

    @SuppressWarnings("EmptyMethod")
    public static void init() {
        // Static fields are now initialized
    }

    public static boolean isFrozen() {
        return frozen;
    }
}
