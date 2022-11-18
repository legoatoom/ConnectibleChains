package com.github.legoatoom.connectiblechains.client.render.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The manager loads the chain models that contain the texture information for all chain types.
 * It looks for models at models/entity/chain/ within the same namespace as the chain type.
 * Inspired by {@link net.minecraft.client.render.model.BakedModelManager} and {@link net.minecraft.client.render.model.ModelLoader}.
 */
public class ChainTextureManager implements SimpleResourceReloadListener<Map<Identifier, ChainTextureManager.JsonModel>> {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Identifier MISSING_ID = new Identifier(ConnectibleChains.MODID, "textures/entity/missing.png");
    /**
     * Maps chain types to chain texture ids.
     */
    private final Object2ObjectMap<Identifier, Identifier> chainTextures = new Object2ObjectOpenHashMap<>(64);
    /**
     * Maps chain types to knot texture ids.
     */
    private final Object2ObjectMap<Identifier, Identifier> knotTextures = new Object2ObjectOpenHashMap<>(64);

    @Override
    public Identifier getFabricId() {
        return Helper.identifier("chain_textures");
    }

    @Override
    public CompletableFuture<Map<Identifier, JsonModel>> load(ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> load(manager));
    }

    /**
     * Loads all models for all registered chain types.
     *
     * @param manager The resource manager
     * @return A map of chain type ids to model data
     */
    public Map<Identifier, JsonModel> load(ResourceManager manager) {
        Map<Identifier, JsonModel> map = new HashMap<>();

        for (Identifier chainType : ChainTypesRegistry.REGISTRY.getIds()) {
            try(Reader reader =  manager.openAsReader(getResourceID(chainType))){
                JsonModel jsonModel = GSON.fromJson(reader, JsonModel.class);
                map.put(chainType, jsonModel);
            } catch (IOException e){
                ConnectibleChains.LOGGER.error("Missing model for {}.", chainType, e);
            }
        }
        return map;
    }

    private static Identifier getResourceID(Identifier modelId) {
        return new Identifier(modelId.getNamespace(), "models/entity/connectiblechains/%s.json".formatted(modelId.getPath()));
    }

    @Override
    public CompletableFuture<Void> apply(Map<Identifier, JsonModel> textureMap, ResourceManager manager, Profiler profiler, Executor executor) {
        chainTextures.clear();
        knotTextures.clear();

        textureMap.forEach((id, entry) -> {
            chainTextures.put(id, entry.textures.chainTextureId());
            knotTextures.put(id, entry.textures.knotTextureId());
        });
        return CompletableFuture.completedFuture(null);
    }

    public Identifier getChainTexture(Identifier chainType) {
        return chainTextures.getOrDefault(chainType, MISSING_ID);
    }

    public Identifier getKnotTexture(Identifier chainType) {
        return knotTextures.getOrDefault(chainType, MISSING_ID);
    }

    /**
     * This class represents the json structure of the model file
     */
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
