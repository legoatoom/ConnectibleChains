package com.github.legoatoom.connectiblechains.compat;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * This class is unused as data driven chain types are not implemented yet.
 * Here's why:
 * - Registries don't seem to support removing entries, so once a dynamic type is loaded
 * it will stay until the game is restarted.
 * - Tags don't get synced before the client and server registries get compared
 * so a custom packet would probably be required.
 * I (qendolin) can think of two solutions:
 * - Use a HashMap (I would rather not)
 * - Recreate the registry when joining a world (This seems to be how vanilla does it)
 *
 * If you know how to do this or have an idea, please create an issue or pull request!
 */
public class DataDrivenCompat implements SimpleResourceReloadListener<Set<Identifier>> {
    public static final String PATH = ConnectibleChains.MODID + "/types.json";
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    @Override
    public Identifier getFabricId() {
        return Helper.identifier("chain_types");
    }

    @Override
    public CompletableFuture<Set<Identifier>> load(ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            if (ChainTypesRegistry.isFrozen()) {
                ConnectibleChains.LOGGER.warn("Dynamic chain types are not supported, a restart is required.");
                return Set.of();
            }

            return load(manager);
        });
    }

    /**
     * Loads all chain types from all namespaces at {@link #PATH} and respects priority.
     * Like {@link net.minecraft.tag.TagGroupLoader}.
     * @param manager The resource manager
     * @return A set of all item ids that should become chain types
     */
    private Set<Identifier> load(ResourceManager manager) {
        Set<Identifier> chainTypes = new HashSet<>();

        for (String ns : manager.getAllNamespaces()) {
            try {
                for (Resource res : manager.getAllResources(new Identifier(ns, PATH))) {
                    try (res) {
                        InputStreamReader reader = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8);
                        String[] strings = GSON.fromJson(reader, String[].class);
                        for (String string : strings) {
                            Identifier id = new Identifier(string);
                            chainTypes.add(id);
                        }
                    } catch (Exception e) {
                        ConnectibleChains.LOGGER.error("Failed to load {}.", res.getId(), e);
                    }
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                ConnectibleChains.LOGGER.error("Failed to load {} for namespace {}.", PATH, ns, e);
            }
        }

        return chainTypes;
    }

    @Override
    public CompletableFuture<Void> apply(Set<Identifier> ids, ResourceManager manager, Profiler profiler, Executor executor) {
        for (Identifier id : ids) {
            Optional<Item> item = Registry.ITEM.getOrEmpty(id);
            if (item.isEmpty()) {
                ConnectibleChains.LOGGER.error("Cannot add type, item {} does not exist.", id);
                continue;
            }
            ChainTypesRegistry.registerDynamic(item.get());
        }

        return CompletableFuture.completedFuture(null);
    }
}
