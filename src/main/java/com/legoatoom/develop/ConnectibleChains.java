package com.legoatoom.develop;


import com.legoatoom.develop.items.TempChainItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ConnectibleChains implements ModInitializer {

    public static final String MODID = "connectiblechains";

    public static final Item TEMP_CHAIN = new TempChainItem(new Item.Settings().group(ItemGroup.MISC));

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "temp_chain"), TEMP_CHAIN);
    }

}
