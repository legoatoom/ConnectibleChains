package com.github.legoatoom.connectiblechains.client.render.entity.model;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.compat.BuiltinCompat;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Inspired by {@link net.minecraft.client.render.model.BakedModelManager} and {@link net.minecraft.client.render.model.ModelLoader}
 */
public class ChainTextureManager implements SimpleResourceReloadListener<Map<Identifier, ChainTextureManager.JsonModel>> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Identifier MISSING_ID = new Identifier(ConnectibleChains.MODID, "textures/entity/missing.png");
    private final Object2ObjectMap<Identifier, Identifier> chainTextures = new Object2ObjectOpenHashMap<>(64);
    private final Object2ObjectMap<Identifier, Identifier> knotTextures = new Object2ObjectOpenHashMap<>(64);
    private final ObjectSet<Identifier> loadedModels = new ObjectArraySet<>();

    public Identifier getChainTexture(Identifier chainType) {
        return chainTextures.getOrDefault(chainType, MISSING_ID);
    }

    public Identifier getKnotTexture(Identifier chainType) {
        return knotTextures.getOrDefault(chainType, MISSING_ID);
    }

    @Override
    public Identifier getFabricId() {
        return Helper.identifier("chain_textures");
    }

    @Override
    public CompletableFuture<Map<Identifier, JsonModel>> load(ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> load(manager, false));
    }

    public Map<Identifier, JsonModel> load(ResourceManager manager, boolean diff) {
        Map<Identifier, JsonModel> map = new HashMap<>();

        for (Identifier chainType : ChainTypesRegistry.REGISTRY.getIds()) {
            if(diff && loadedModels.contains(chainType)) continue;
            try (Resource resource = manager.getResource(getResourceId(getModelId(chainType)))) {
                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                JsonModel jsonModel = GSON.fromJson(reader, JsonModel.class);
                map.put(chainType, jsonModel);
            } catch (FileNotFoundException e) {
                ConnectibleChains.LOGGER.error("Missing model for {}.", chainType, e);
            } catch (Exception e) {
                ConnectibleChains.LOGGER.error("Failed to load model for {}.", chainType, e);
            }
        }

        return map;
    }

    /**
     * @see net.minecraft.data.client.model.Texture#getId(Item)
     * @see net.minecraft.data.client.model.ModelIds#getItemModelId(Item)
     */
    public static Identifier getModelId(Identifier chainType) {
        return new Identifier(chainType.getNamespace(), "entity/chain/" + chainType.getPath());
    }

    public static Identifier getResourceId(Identifier modelId) {
        return new Identifier(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
    }
    
    @Override
    public CompletableFuture<Void> apply(Map<Identifier, JsonModel> textureMap, ResourceManager manager, Profiler profiler, Executor executor) {
        apply(textureMap, false);
        return CompletableFuture.completedFuture(null);
    }

    public void apply(Map<Identifier, JsonModel> textureMap, boolean diff) {
        if(!diff) {
            chainTextures.clear();
            knotTextures.clear();
            loadedModels.clear();
        }

        textureMap.forEach((id, entry) -> {
            if(diff && loadedModels.contains(id)) return;
            chainTextures.put(id, entry.textures.chainTextureId());
            knotTextures.put(id, entry.textures.knotTextureId());
            loadedModels.add(id);
        });
    }

    protected static final class JsonModel {
        public Textures textures;

        protected static final class Textures {
            public String chain;
            public String knot;

            public Identifier chainTextureId() {
                return new Identifier(chain + ".png");
            }

            public Identifier knotTextureId() {
                return new Identifier(knot + ".png");
            }
        }
    }
}
