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
import com.github.legoatoom.connectiblechains.util.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/**
 * The manager loads the chain models that contain the texture information for all chain types.
 * It looks for models at models/entity/chain/ within the same namespace as the chain type.
 * Inspired by {@link net.minecraft.client.render.model.BakedModelManager} and {@link net.minecraft.client.render.model.ModelLoader}.
 */
public class ChainTextureManager implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Identifier MISSING_ID = new Identifier(ConnectibleChains.MODID, "textures/entity/missing.png");
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

    @Override
    public void reload(ResourceManager manager) {
        clearCache();

        for (Item item : Registries.ITEM) {
            if (!Registries.ITEM.getEntry(item).isIn(ConventionalItemTags.CHAINS)) {
                continue;
            }

            Identifier itemId = Registries.ITEM.getId(item);
            Optional<Resource> optionalResource = manager.getResource(new Identifier(itemId.getNamespace(), "%s/%s.json".formatted(MODEL_FILE_LOCATION, itemId.getPath())));
            // default texture locations.
            Identifier chainTextureId = new Identifier(itemId.getNamespace(), "textures/block/%s.png".formatted(itemId.getPath()));
            Identifier knotTextureId = new Identifier(itemId.getNamespace(), "textures/item/%s.png".formatted(itemId.getPath()));
            if (optionalResource.isEmpty()) {
                ConnectibleChains.LOGGER.warn("Unable to find model file for {}, will assume default", itemId);
            } else {
                Resource resource = optionalResource.get();
                try (Reader reader = resource.getReader()) {
                    JsonModel model = GSON.fromJson(reader, JsonModel.class);
                    chainTextureId = model.textures.chainTextureId();
                    knotTextureId = model.textures.knotTextureId();
                } catch (IOException e) {
                    ConnectibleChains.LOGGER.warn("Error opening model file for {}, will assume default", itemId);
                }
            }
            chainTextures.put(itemId, chainTextureId);
            knotTextures.put(itemId, knotTextureId);
        }
    }

    public void clearCache() {
        ClientInitializer.getInstance()
                .getChainKnotEntityRenderer()
                .ifPresent(it -> it.getChainRenderer().purge());
        chainTextures.clear();
        knotTextures.clear();

    }

    public Identifier getChainTexture(Identifier sourceItemId) {
        return chainTextures.computeIfAbsent(sourceItemId, (Identifier id) -> {
            // Default location.
            ConnectibleChains.LOGGER.warn("Did not find a model file for the chain '%s', assuming default path.".formatted(sourceItemId));
            return new Identifier(id.getNamespace(), "textures/block/%s.png".formatted(id.getPath()));
        });
    }

    public Identifier getKnotTexture(Identifier sourceItemId) {
        return knotTextures.computeIfAbsent(sourceItemId, (Identifier id) -> {
            // Default location.
            ConnectibleChains.LOGGER.warn("Did not find a model file for the chain '%s', assuming default path.".formatted(sourceItemId));
            return new Identifier(id.getNamespace(), "textures/item/%s.png".formatted(id.getPath()));
        });
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
