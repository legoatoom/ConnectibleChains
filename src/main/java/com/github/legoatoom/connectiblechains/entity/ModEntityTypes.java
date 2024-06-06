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

package com.github.legoatoom.connectiblechains.entity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * This class keeps track of all entities that this mod has.
 * It also registers them.
 *
 * @author legoatoom
 */
public class ModEntityTypes {

    public static final EntityType<ChainKnotEntity> CHAIN_KNOT;
    public static final EntityType<ChainCollisionEntity> CHAIN_COLLISION;

    static {
        CHAIN_KNOT = Registry.register(
                Registries.ENTITY_TYPE, Helper.identifier("chain_knot"),
                EntityType.Builder.create((EntityType.EntityFactory<ChainKnotEntity>) ChainKnotEntity::new, SpawnGroup.MISC)
                        .trackingTickInterval(Integer.MAX_VALUE).alwaysUpdateVelocity(false)
                        .dimensions(0.375f, 0.5F)
                        .spawnableFarFromPlayer()
                        .makeFireImmune()
                        .build("chain_knot")
        );
        CHAIN_COLLISION = Registry.register(
                Registries.ENTITY_TYPE, Helper.identifier("chain_collision"),
                EntityType.Builder.create((EntityType.EntityFactory<ChainCollisionEntity>) ChainCollisionEntity::new, SpawnGroup.MISC)
                        .maxTrackingRange(1).trackingTickInterval(Integer.MAX_VALUE).alwaysUpdateVelocity(false)
                        .dimensions(0.25f, 0.375f)
                        .disableSaving()
                        .disableSummon()
                        .makeFireImmune()
                        .build("chain_collision")
        );

    }

    public static void init() {
        ConnectibleChains.LOGGER.info("Initialized entity types.");
    }
}
