/*
 * Copyright (C) 2025 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.client.render.entity.catenary;

import com.github.legoatoom.connectiblechains.client.render.entity.UVRect;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.Optional;

public record CatenaryModel(Optional<CatenaryTextures> textures, Optional<Identifier> catenaryRendererId,
                            Optional<Pair<UVRect, UVRect>> uvRects) {
    public static final MapCodec<CatenaryModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CatenaryTextures.CODEC.codec().optionalFieldOf("textures").forGetter(CatenaryModel::textures),
            Identifier.CODEC.optionalFieldOf("model").forGetter(CatenaryModel::catenaryRendererId),
            UVRect.CODEC.optionalFieldOf("uv").forGetter(CatenaryModel::uvRects)
    ).apply(instance, CatenaryModel::new));

    public record CatenaryTextures(Optional<Identifier> chainTexture, Optional<Identifier> knotTexture) {
        public static final MapCodec<CatenaryTextures> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Identifier.CODEC.optionalFieldOf("chain").forGetter(CatenaryTextures::chainTexture),
                Identifier.CODEC.optionalFieldOf("knot").forGetter(CatenaryTextures::knotTexture)
        ).apply(instance, CatenaryTextures::new));
    }
}