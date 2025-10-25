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

package com.github.legoatoom.connectiblechains.migrator;


import com.github.legoatoom.connectiblechains.migrator.migrations.ChainToNewSystem;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import net.minecraft.entity.decoration.BlockAttachedEntity;

public class ChainableMigrator<E extends BlockAttachedEntity & Chainable> extends DataMigrator<E> {

    @Override
    public void registerMigrations() {
        registerMigration(1_00_00, ChainToNewSystem::migrate);
    }
}
