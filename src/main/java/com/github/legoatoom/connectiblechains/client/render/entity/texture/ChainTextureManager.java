/*
 * Copyright (C) 2024 legoatoom
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity.texture;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.CrossCatenaryRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.ICatenaryRenderer;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * The manager loads the chain models that contain the texture information for all chain types.
 * It looks for models at models/entity/chain/ within the same namespace as the chain type.
 */
public class ChainTextureManager extends JsonDataLoader<JsonElement> implements IdentifiableResourceReloadListener {
    private static final String MODEL_FILE_LOCATION = "models/entity/" + ConnectibleChains.MODID;
    /**
     * How many different chain items do we expect?
     */
    private static final int EXPECTED_UNIQUE_CHAIN_COUNT = 64;
    /**
     * Maps chain types to chain texture ids.
     */
    private final Object2ObjectMap<Identifier, Identifier> chainTextures = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);
    /**
     * Maps chain types to knot texture ids.
     */
    private final Object2ObjectMap<Identifier, Identifier> knotTextures = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);

    @Override
    public Identifier getFabricId() {
        return Helper.identifier("chain_models");
    }

    public ChainTextureManager() {
        super(Codecs.JSON_ELEMENT, ResourceFinder.json(MODEL_FILE_LOCATION));
    }

    @Override
    public void apply(Map<Identifier, JsonElement> data, ResourceManager manager, Profiler profiler) {
        clearCache();
        data.forEach((identifier, jsonElement) -> {
            Pair<Identifier, Identifier> textures = extractChainTextures(identifier, jsonElement);
            chainTextures.put(identifier, textures.getLeft());
            knotTextures.put(identifier, textures.getRight());
        });
    }

    private static Pair<Identifier, Identifier> extractChainTextures(Identifier itemId, JsonElement jsonElement) {
        //Default
        Identifier chainTextureId = defaultChainTextureId(itemId);
        Identifier knotTextureId = defaultKnotTextureId(itemId);

        if (jsonElement.isJsonObject()) {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonObject texturesObject = jsonObject.getAsJsonObject("textures");

            if (texturesObject.has("chain") && texturesObject.get("chain").isJsonPrimitive()) {
                chainTextureId = Identifier.tryParse(texturesObject.get("chain").getAsString()+ ".png");
            }
            if (texturesObject.has("knot") && texturesObject.get("knot").isJsonPrimitive()) {
                knotTextureId = Identifier.tryParse(texturesObject.get("knot").getAsString()+ ".png");
            }
        }

        return new Pair<>(chainTextureId, knotTextureId);
    }

    public void clearCache() {
        ClientInitializer.getInstance()
                .getChainKnotEntityRenderer()
                .ifPresent(it -> it.getChainRenderer().purge());
        chainTextures.clear();
        knotTextures.clear();
    }


    private static @NotNull Identifier defaultChainTextureId(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "textures/block/%s.png".formatted(itemId.getPath()));
    }
    private static @NotNull Identifier defaultKnotTextureId(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "textures/item/%s.png".formatted(itemId.getPath()));
    }


    public ICatenaryRenderer getCatenaryRenderer(Identifier sourceItemId) {
        return new CrossCatenaryRenderer();
    }

    public Identifier getChainTexture(Identifier sourceItemId) {
        return chainTextures.computeIfAbsent(sourceItemId, (Identifier id) -> {
            // Default location.
            ConnectibleChains.LOGGER.warn("Did not find a model file for the chain '%s', assuming default path.".formatted(sourceItemId));
            return defaultChainTextureId(id);
        });
    }

    public Identifier getKnotTexture(Identifier sourceItemId) {
        return knotTextures.computeIfAbsent(sourceItemId, (Identifier id) -> {
            // Default location.
            ConnectibleChains.LOGGER.warn("Did not find a model file for the chain '%s', assuming default path.".formatted(sourceItemId));
            return defaultKnotTextureId(id);
        });
    }
}
