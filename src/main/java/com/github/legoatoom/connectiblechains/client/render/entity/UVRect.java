/*
 * Copyright (C) 2024 legoatoom.
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

package com.github.legoatoom.connectiblechains.client.render.entity;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Pair;

import java.util.List;

/**
 * Specifies the uv coordinates that the renderer should use.
 * The chain texture has to be vertical. Like how the vanilla chain texture works for blocks.
 */
@Environment(EnvType.CLIENT)
public record UVRect(float x0, float x1) {
    public static final Codec<Pair<UVRect, UVRect>> CODEC = Codec.FLOAT.listOf().xmap(UVRect::to, UVRect::from);

    /**
     * Default UV's for side A
     */
    public static final UVRect DEFAULT_SIDE_A = new UVRect(0, 3);
    /**
     * Default UV's for side B
     */
    public static final UVRect DEFAULT_SIDE_B = new UVRect(3, 6);

    private static Pair<UVRect, UVRect> to(List<Float> floats) {
        return new Pair<>(new UVRect(floats.get(0), floats.get(1)), new UVRect(floats.get(2), floats.get(3)));
    }

    private static List<Float> from(Pair<UVRect, UVRect> uvRect) {
        return List.of(uvRect.getLeft().x0(), uvRect.getLeft().x1(), uvRect.getRight().x0(), uvRect.getRight().x1());
    }
}
