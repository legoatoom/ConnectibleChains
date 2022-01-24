package com.github.legoatoom.connectiblechains.compat;

import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

record SidedResourceReloadListenerProxy<T>(
        @NotNull ResourceType type,
        @NotNull SidedResourceReloadListener<T> impl
) implements SimpleResourceReloadListener<T> {
    public SidedResourceReloadListenerProxy {
        Objects.requireNonNull(type);
        Objects.requireNonNull(impl);
    }

    @Override
    public CompletableFuture<T> load(ResourceManager manager, Profiler profiler, Executor executor) {
        return impl.load(type, manager, profiler, executor);
    }

    @Override
    public CompletableFuture<Void> apply(T data, ResourceManager manager, Profiler profiler, Executor executor) {
        return impl.apply(type, data, manager, profiler, executor);
    }

    @Override
    public Identifier getFabricId() {
        return impl.getFabricId();
    }

    @Override
    public Collection<Identifier> getFabricDependencies() {
        return impl.getFabricDependencies();
    }

    @Override
    public String getName() {
        return impl.getName();
    }
}
