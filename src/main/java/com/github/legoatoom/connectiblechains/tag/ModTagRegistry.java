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

package com.github.legoatoom.connectiblechains.tag;

import com.github.legoatoom.connectiblechains.util.Helper;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTagRegistry {
    public static final TagKey<Block> CHAIN_CONNECTIBLE = makeTag(RegistryKeys.BLOCK, Helper.identifier("chain_connectible"));
    public static final TagKey<Item> CATENARY_ITEMS = makeTag(RegistryKeys.ITEM, Helper.identifier("catenary_items"));

    /**
     * Get or create {@link TagKey}.
     *
     * @param registry Tag collection;
     * @param id       The tag id.
     * @return An existing or new {@link TagKey}.
     */
    public static <T> TagKey<T> makeTag(RegistryKey<Registry<T>> registry, Identifier id) {
        return TagKey.of(registry, id);
    }
}
