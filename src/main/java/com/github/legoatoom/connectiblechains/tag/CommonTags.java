/*
 * Copyright (C) 2023 legoatoom
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

package com.github.legoatoom.connectiblechains.tag;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * @see <a href="https://github.com/paulevsGitch/BCLib/blob/1.18.2/src/main/java/ru/bclib/api/tag/TagAPI.java">github.com/paulevsGitch/BCLib</>
 */
public class CommonTags {

    public static final TagKey<Item> SHEARS = makeCommonItemTag("shears");
    public static final TagKey<Item> CHAINS = makeCommonItemTag("chains");
    public static final TagKey<Block> BARS = makeCommonBlockTag("bars");

    /**
     * Get or create {@link Item} {@link TagKey}.
     *
     * @param name The tag name / path.
     * @return An existing or new {@link TagKey}.
     * @see <a href="https://fabricmc.net/wiki/tutorial:tags">Fabric Wiki (Tags)</a>
     */
    public static TagKey<Item> makeCommonItemTag(String name) {
        return makeTag(Registries.ITEM, new Identifier("c", name));
    }


    /**
     * Get or create {@link Block} {@link TagKey}.
     *
     * @param name The tag name / path.
     * @return An existing or new {@link TagKey}.
     * @see <a href="https://fabricmc.net/wiki/tutorial:tags">Fabric Wiki (Tags)</a>
     */
    public static TagKey<Block> makeCommonBlockTag(String name) {
        return makeTag(Registries.BLOCK, new Identifier("c", name));
    }



    /**
     * Get or create {@link TagKey}.
     *
     * @param registry Tag collection;
     * @param id       The tag id.
     * @return An existing or new {@link TagKey}.
     */
    public static <T> TagKey<T> makeTag(DefaultedRegistry<T> registry, Identifier id) {
        return registry
                .streamTags()
                .filter(tagKey -> tagKey.id().equals(id))
                .findAny()
                .orElse(TagKey.of(registry.getKey(), id));
    }

    public static boolean isShear(ItemStack tool) {
        return tool.isOf(Items.SHEARS) | tool.isIn(SHEARS);
    }

    public static boolean isChain(ItemStack itemStack) {
        return itemStack.isIn(CHAINS);
    }


}
