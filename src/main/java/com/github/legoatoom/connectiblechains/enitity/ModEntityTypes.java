/*
 *     Copyright (C) 2020 legoatoom
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.registry.Registry;

public class ModEntityTypes {

    public static final EntityType<ChainKnotEntity> CHAIN_KNOT;
    public static final EntityType<ChainCollisionEntity> CHAIN_COLLISION;

    static{
        CHAIN_KNOT = Registry.register(
                Registry.ENTITY_TYPE, Helper.identifier("chain_knot") ,
                FabricEntityTypeBuilder.create(SpawnGroup.MISC,
                        (EntityType.EntityFactory<ChainKnotEntity>) ChainKnotEntity::new)
                        .trackRangeBlocks(10).trackedUpdateRate(Integer.MAX_VALUE).forceTrackedVelocityUpdates(false)
                        .dimensions(EntityDimensions.fixed(0.5F, 0.5F))
                        .spawnableFarFromPlayer()
                        .fireImmune()
                        .build()
        );
        CHAIN_COLLISION = Registry.register(
                Registry.ENTITY_TYPE, Helper.identifier("chain_collision") ,
                FabricEntityTypeBuilder.create(SpawnGroup.MISC,
                        (EntityType.EntityFactory<ChainCollisionEntity>) ChainCollisionEntity::new)
                        .trackRangeBlocks(10).trackedUpdateRate(Integer.MAX_VALUE).forceTrackedVelocityUpdates(false)
                        .dimensions(EntityDimensions.fixed(0.3F, 0.3F))
                        .disableSaving()
                        .fireImmune()
                        .spawnableFarFromPlayer()
                        .build()
        );

    }

    @SuppressWarnings("EmptyMethod")
    public static void init(){}
}
