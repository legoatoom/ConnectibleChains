package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.registry.Registry;

public class ModEntityTypes {

    public static final EntityType<ChainKnotEntity> CHAIN_KNOT;

    static{
        CHAIN_KNOT = Registry.register(
                            Registry.ENTITY_TYPE, Helper.identifier("chain_knot") ,
                            FabricEntityTypeBuilder.create(SpawnGroup.MISC,
                                    (EntityType.EntityFactory<ChainKnotEntity>) ChainKnotEntity::new)
                                    .trackable(10, Integer.MAX_VALUE, false)
                                    .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
                                    .build()
                    );
    }

    public static void init(){}
}
