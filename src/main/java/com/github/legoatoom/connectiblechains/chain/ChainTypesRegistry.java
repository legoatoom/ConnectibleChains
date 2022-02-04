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
import net.minecraft.util.registry.RegistryKey;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class ChainTypesRegistry {
    public static final Identifier DEFAULT_CHAIN_TYPE_ID = Helper.identifier("iron_chain");
    public static final RegistryKey<? extends Registry<ChainType>> CHAIN_TYPE_KEY =
            RegistryKey.ofRegistry(Helper.identifier("chain_types"));
    public static final DefaultedRegistry<ChainType> REGISTRY = FabricRegistryBuilder.from(
                    new DefaultedRegistry<>(DEFAULT_CHAIN_TYPE_ID.toString(), CHAIN_TYPE_KEY, Lifecycle.stable()))
            .attribute(RegistryAttribute.SYNCED).buildAndRegister();

    public static final Map<Item, ChainType> ITEM_CHAIN_TYPES = new Object2ObjectOpenHashMap<>(64);

    public static final ChainType DEFAULT_CHAIN_TYPE = register(DEFAULT_CHAIN_TYPE_ID, Items.CHAIN);
    @SuppressWarnings("unused")
    public static final ChainType IRON_CHAIN = DEFAULT_CHAIN_TYPE;
    private static boolean locked = false;

    /**
     * Prevents registration with {@link #registerDynamic(Item)}
     */
    public static void lock() {
        locked = true;
    }

    /**
     * Used to register chain types on initialization. Cannot register a type twice.
     * @param item the chain type's item
     * @return the new {@link ChainType}
     */
    @SuppressWarnings("UnusedReturnValue")
    public static ChainType register(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        if (id == Registry.ITEM.getDefaultId()) {
            throw new AssertionError("Cannot create chain type with unregistered item");
        }
        if (REGISTRY.containsId(id)) return REGISTRY.get(id);
        ChainType chainType = Registry.register(REGISTRY, id, new ChainType(item));
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }

    /**
     * Used to register chain types after initialization.
     * Can replace existing chain types.
     * @param item the chain type's item
     */
    public static void registerDynamic(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        if (locked) {
            ConnectibleChains.LOGGER.error("Tried to add {} but registry is locked.", id);
        }
        Optional<ChainType> existing = REGISTRY.getOrEmpty(id);
        OptionalInt rawId = OptionalInt.empty();
        if (existing.isPresent()) {
            if (existing.get().item() == item) return;
            rawId = OptionalInt.of(REGISTRY.getRawId(existing.get()));
        }

        ChainType chainType = REGISTRY.replace(rawId, RegistryKey.of(REGISTRY.getKey(), id), new ChainType(item), Lifecycle.stable());
        ITEM_CHAIN_TYPES.put(item, chainType);
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

    public static boolean isLocked() {
        return locked;
    }
}
