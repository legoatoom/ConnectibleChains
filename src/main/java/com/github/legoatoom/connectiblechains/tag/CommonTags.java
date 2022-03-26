package com.github.legoatoom.connectiblechains.tag;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * @see <a href="https://github.com/paulevsGitch/BCLib/blob/1.18.2/src/main/java/ru/bclib/api/tag/TagAPI.java">github.com/paulevsGitch/BCLib</>
 */
public class CommonTags {

    public static final TagKey<Item> SHEARS = makeCommonItemTag("shears");

    /**
     * Get or create {@link Item} {@link TagKey}.
     *
     * @param name The tag name / path.
     * @return An existing or new {@link TagKey}.
     * @see <a href="https://fabricmc.net/wiki/tutorial:tags">Fabric Wiki (Tags)</a>
     */
    public static TagKey<Item> makeCommonItemTag(String name) {
        return makeTag(Registry.ITEM, new Identifier("c", name));
    }

    /**
     * Get or create {@link TagKey}.
     *
     * @param registry Tag collection;
     * @param id       The tag id.
     * @return An existing or new {@link TagKey}.
     */
    public static <T> TagKey<T> makeTag(Registry<T> registry, Identifier id) {
        return registry
                .streamTags()
                .filter(tagKey -> tagKey.id().equals(id))
                .findAny()
                .orElse(TagKey.of(registry.getKey(), id));
    }

    public static boolean isShear(ItemStack tool) {
        return tool.isOf(Items.SHEARS) | tool.isIn(SHEARS);
    }

}
