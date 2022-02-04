package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.util.Helper;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class ChainTypesRegistry {
    public static final Identifier CHAINABLE_TAG_ID = Helper.identifier("chainable");
    public static final Tag<Item> CHAINABLE_TAG = TagFactory.ITEM.create(CHAINABLE_TAG_ID);

    public static final Identifier DEFAULT_CHAIN_TYPE_ID = Helper.identifier("iron_chain");
    public static final RegistryKey<? extends Registry<ChainType>> CHAIN_TYPE_KEY =
            RegistryKey.ofRegistry(Helper.identifier("chainable"));
    public static final DefaultedRegistry<ChainType> REGISTRY = FabricRegistryBuilder.from(
            new DefaultedRegistry<>(DEFAULT_CHAIN_TYPE_ID.toString(), CHAIN_TYPE_KEY, Lifecycle.stable()))
            .attribute(RegistryAttribute.SYNCED).buildAndRegister();

    public static final Map<Item, ChainType> ITEM_CHAIN_TYPES = new Object2ObjectOpenHashMap<>(64);

    public static final ChainType DEFAULT_CHAIN_TYPE = register(DEFAULT_CHAIN_TYPE_ID, Items.CHAIN);
    public static final ChainType IRON_CHAIN = DEFAULT_CHAIN_TYPE;

    public static ChainType register(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        if(id == Registry.ITEM.getDefaultId()) return null;
        if(REGISTRY.containsId(id)) return REGISTRY.get(id);
        ChainType chainType = Registry.register(REGISTRY, id, new ChainType(item));
        ITEM_CHAIN_TYPES.put(item, chainType);
        return chainType;
    }

    public static void registerDynamic(Item item) {
        Identifier id = Registry.ITEM.getId(item);
        Optional<ChainType> existing = REGISTRY.getOrEmpty(id);
        OptionalInt rawId = OptionalInt.empty();
        if(existing.isPresent()) {
            if(existing.get().item() == item) return;
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

    public static void init() {
        // Static fields are now initialized
    }
}
