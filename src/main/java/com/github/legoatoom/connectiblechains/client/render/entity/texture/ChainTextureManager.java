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
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.CatenaryModel;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.github.legoatoom.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * The manager loads the chain models that contain the texture information for all chain types.
 * It looks for models at models/entity/chain/ within the same namespace as the chain type.
 */
public class ChainTextureManager extends JsonDataLoader<CatenaryModel> implements IdentifiableResourceReloadListener {
    private static final String MODEL_FILE_LOCATION = "models/entity/" + ConnectibleChains.MODID;
    /**
     * How many different chain items do we expect?
     */
    private static final int EXPECTED_UNIQUE_CHAIN_COUNT = 64;

    private Map<Identifier, CatenaryModel> models = new Object2ObjectOpenHashMap<>(EXPECTED_UNIQUE_CHAIN_COUNT);


    @Override
    public Identifier getFabricId() {
        return Helper.identifier("chain_models");
    }

    public ChainTextureManager() {
        super(CatenaryModel.CODEC.codec(), ResourceFinder.json(MODEL_FILE_LOCATION));
    }

    @Override
    protected void apply(Map<Identifier, CatenaryModel> prepared, ResourceManager manager, Profiler profiler) {
        this.models = prepared;
    }

    private static @NotNull Identifier defaultChainTextureId(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "block/%s.png".formatted(itemId.getPath()));
    }
    private static @NotNull Identifier defaultKnotTextureId(Identifier itemId) {
        return Identifier.of(itemId.getNamespace(), "item/%s.png".formatted(itemId.getPath()));
    }

    public CatenaryRenderer getCatenaryRenderer(Identifier sourceItemId) {
        return CatenaryRenderer.getRenderer(Optional.ofNullable(models.get(sourceItemId)).flatMap(CatenaryModel::catenaryRendererId).orElseGet(() -> Helper.identifier("cross")));
    }

    public Identifier getChainTexture(Identifier sourceItemId) {
        return Optional.ofNullable(models.get(sourceItemId)).flatMap(CatenaryModel::textures).flatMap(CatenaryModel.CatenaryTextures::chainTexture).orElseGet(() -> defaultChainTextureId(sourceItemId)).withPath("textures/%s.png"::formatted);
    }

    public Identifier getKnotTexture(Identifier sourceItemId) {
        return Optional.ofNullable(models.get(sourceItemId)).flatMap(CatenaryModel::textures).flatMap(CatenaryModel.CatenaryTextures::knotTexture).orElseGet(() -> defaultKnotTextureId(sourceItemId)).withPath("textures/%s.png"::formatted);
    }
}
