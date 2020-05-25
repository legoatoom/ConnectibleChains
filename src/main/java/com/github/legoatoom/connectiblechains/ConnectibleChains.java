package com.github.legoatoom.connectiblechains;


import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.items.TempChainItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ConnectibleChains implements ModInitializer {

    public static final String MODID = "connectiblechains";

    public static final Item TEMP_CHAIN = new TempChainItem(new Item.Settings().group(ItemGroup.MISC));

    public static final EntityType<ChainKnotEntity> CHAIN_KNOT =
            Registry.register(
                    Registry.ENTITY_TYPE, new Identifier(ConnectibleChains.MODID,  "chain_knot"),
                    FabricEntityTypeBuilder.create(SpawnGroup.MISC,
                            (EntityType.EntityFactory<ChainKnotEntity>) ChainKnotEntity::new)
                            .trackable(10, Integer.MAX_VALUE, false)
                            .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
                            .build()
            );

    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "temp_chain"), TEMP_CHAIN);
    }

}
