package com.github.legoatoom.connectiblechains.compat;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface SidedResourceReloadListener<T> {
    CompletableFuture<T> load(ResourceType type, ResourceManager manager, Profiler profiler, Executor executor);
    CompletableFuture<Void> apply(ResourceType type, T data, ResourceManager manager, Profiler profiler, Executor executor);

    Identifier getFabricId();

    default Collection<Identifier> getFabricDependencies() {
        return Collections.emptyList();
    }

    default String getName() {
        return this.getClass().getSimpleName();
    }

    static <T> SimpleResourceReloadListener<T> proxy(ResourceType type, SidedResourceReloadListener<T> impl) {
        return new SidedResourceReloadListenerProxy<T>(type, impl);
    }
}

