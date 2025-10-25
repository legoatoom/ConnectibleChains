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

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import net.minecraft.entity.Entity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Data fixers in minecraft are a pain to understand.
 * Therefor I won't bother with them and create something simple my owner.
 * <p>
 * This system basically works like migrations in rails.
 * Where you keep going migrating until you are to the current version.
 * This is then done when the reading the entity.
 */
public abstract class DataMigrator<Context extends Entity> {

    private static final String DATA_VERSION_KEY = ConnectibleChains.MODID + "_DataVersion";

    private final SortedMap<Integer, Migration<Context>> migrations = new TreeMap<>();

    public DataMigrator() {
        registerMigrations();
    }

    public abstract void registerMigrations();

    public ReadView migrate(ReadView readView, Context context) {
        int version = readView.getInt(DATA_VERSION_KEY, 0);
        ReadView currentReadView = readView;
        try (ErrorReporter.Logging logging = new ErrorReporter.Logging(context.getErrorReporterContext(), ConnectibleChains.LOGGER)) {
            for (Map.Entry<Integer, Migration<Context>> entry : migrations.entrySet()) {
                Integer migrationVersion = entry.getKey();
                Migration<Context> migration = entry.getValue();

                if (version >= migrationVersion) break; // Done Migrating

                NbtWriteView newWriteView = NbtWriteView.create(logging);

                try {
                    migration.migrate(currentReadView, newWriteView, context);
                } catch (Exception e) {
                    ConnectibleChains.LOGGER.error("Error during fixing {} for '{}':", context, version, e);
                }

                currentReadView = NbtReadView.create(logging, context.getRegistryManager(), newWriteView.getNbt());
            }
        }
        return currentReadView;
    }

    public void addVersionTag(WriteView writeView) {
        writeView.putInt(DATA_VERSION_KEY, getLatestVersion());
    }

    private int getLatestVersion() {
        return migrations.lastKey();
    }

    protected void registerMigration(int version, Migration<Context> migration) {
        if (!migrations.containsKey(version)) {
            migrations.put(version, migration);
        } else {
            ConnectibleChains.LOGGER.error("Version {} already registered!", version);
        }
    }
}
