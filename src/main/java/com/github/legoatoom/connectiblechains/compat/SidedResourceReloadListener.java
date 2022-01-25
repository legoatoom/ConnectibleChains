package com.github.legoatoom.connectiblechains.compat;

import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A reload-listener that is aware of the resource type.
 *
 * @param <T> Type of data that is loaded by this listener
 * @author gudenau, Qendolin
 * @see SimpleResourceReloadListener
 */
@SuppressWarnings("unused")
public interface SidedResourceReloadListener<T> {
    static <T> SimpleResourceReloadListener<T> proxy(ResourceType type, SidedResourceReloadListener<T> impl) {
        return new SidedResourceReloadListenerProxy<>(type, impl);
    }

    Identifier getFabricId();

    default Collection<Identifier> getFabricDependencies() {
        return Collections.emptyList();
    }

    default String getName() {
        return this.getClass().getSimpleName();
    }

    CompletableFuture<T> load(ResourceType type, ResourceManager manager, Profiler profiler, Executor executor);

    CompletableFuture<Void> apply(ResourceType type, T data, ResourceManager manager, Profiler profiler, Executor executor);
}

