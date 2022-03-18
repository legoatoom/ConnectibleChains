package com.github.legoatoom.connectiblechains.client.render.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.compat.BuiltinCompat;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
     * @param manager The resource manager
     * @return A map of chain type ids to model data
     */
    public Map<Identifier, JsonModel> load(ResourceManager manager) {
        Map<Identifier, JsonModel> map = new HashMap<>();

        for (Identifier chainType : ChainTypesRegistry.REGISTRY.getIds()) {
            try (Resource resource = manager.getResource(getResourceId(getModelId(chainType)))) {
                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                JsonModel jsonModel = GSON.fromJson(reader, JsonModel.class);
                map.put(chainType, jsonModel);
            } catch (FileNotFoundException e) {
                JsonModel builtinModel = loadBuiltinModel(manager, chainType);
                if(builtinModel != null) {
                    map.put(chainType, builtinModel);
                } else {
                    ConnectibleChains.LOGGER.error("Missing model for {}.", chainType, e);
                }
            } catch (Exception e) {
                ConnectibleChains.LOGGER.error("Failed to load model for {}.", chainType, e);
            }
        }

        return map;
    }

    /**
     * Checks if {@code chainType} is a builtin type and tries to load it's model
     * @param manager The resource manager
     * @param chainType A chain type, can be a builtin type or not
     * @return The model for {@code chainType} or null of none exists
     */
    @Nullable
    private JsonModel loadBuiltinModel(ResourceManager manager, Identifier chainType) {
        if(BuiltinCompat.BUILTIN_TYPES.contains(chainType)) {
            try (Resource resource = manager.getResource(getBuiltinResourceId(getModelId(chainType)))) {
                Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
                return GSON.fromJson(reader, JsonModel.class);
            } catch (Exception e) {
                ConnectibleChains.LOGGER.error("Error for builtin type {}.", chainType, e);
            }
        }
        return null;
    }

    public static Identifier getResourceId(Identifier modelId) {
        return new Identifier(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
    }

    private static Identifier getBuiltinResourceId(Identifier modelId) {
        return new Identifier(ConnectibleChains.MODID, "models/" + modelId.getPath() + ".json");
    }

    /**
     * @see net.minecraft.data.client.model.Texture#getId(Item)
     * @see net.minecraft.data.client.model.ModelIds#getItemModelId(Item)
     */
    public static Identifier getModelId(Identifier chainType) {
        return new Identifier(chainType.getNamespace(), "entity/chain/" + chainType.getPath());
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
    @SuppressWarnings("unused")
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
