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

package com.github.legoatoom.connectiblechains.migrator.migrations;


import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.entity.Chainable;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Migrate the old data to the new system of codecs.
 */
public final class ChainToNewSystem {
    private final static String OLD_CHAINS_NBT_KEY = "Chains";
    private final static String NEW_CHAINS_NBT_KEY = "%s_%s".formatted(ConnectibleChains.MODID, OLD_CHAINS_NBT_KEY);
    private final static String OLD_SOURCE_ITEM_KEY = "SourceItem";
    private final static String NEW_SOURCE_ITEM_KEY = "%s_%s".formatted(ConnectibleChains.MODID, OLD_SOURCE_ITEM_KEY);


    public static <E extends BlockAttachedEntity & Chainable> void  migrate(ReadView readView, WriteView writeView, E entity) {
        Item knotSourceItem = readView.getOptionalString(OLD_SOURCE_ITEM_KEY).map(sourceKey -> Registries.ITEM.get(Identifier.tryParse(sourceKey))).orElse(Items.IRON_CHAIN);
        writeView.put(NEW_SOURCE_ITEM_KEY, Registries.ITEM.getCodec(), knotSourceItem);

        Optional<ReadView.ListReadView> optionalList = readView.getOptionalListReadView(OLD_CHAINS_NBT_KEY);
        optionalList.ifPresent(readViews -> {
            WriteView.ListView linksTag = writeView.getList(NEW_CHAINS_NBT_KEY);

            for (ReadView element : optionalList.get()) {
                WriteView tag = linksTag.add();
                migrateChainData(element, tag, entity);
            }
        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static <E extends BlockAttachedEntity & Chainable> void migrateChainData(ReadView readView, WriteView writeView, E entity) {
        Item source = readView.getOptionalString(OLD_SOURCE_ITEM_KEY).map(sourceKey -> Registries.ITEM.get(Identifier.tryParse(sourceKey))).orElse(Items.IRON_CHAIN);
        writeView.put(NEW_SOURCE_ITEM_KEY, Registries.ITEM.getCodec(), source);

        Optional<String> optionalUUID = readView.getOptionalString("UUID");
        Optional<Integer> optionalDest = readView.getOptionalInt("DestX");
        Optional<Integer> optionalRel = readView.getOptionalInt("RelX");
        if (optionalUUID.isPresent()) {
            UUID uuid = UUID.fromString(optionalUUID.get());
            writeView.put("UUID", Uuids.CODEC, uuid);
        } else if (optionalDest.isPresent()) {
            Integer destX = optionalDest.get();
            Integer destY = readView.getOptionalInt("DestY").get();
            Integer destZ = readView.getOptionalInt("DestZ").get();
            BlockPos desPos = new BlockPos(destX, destY, destZ);
            BlockPos relPos = desPos.subtract(entity.getAttachedBlockPos());
            writeView.put("RelativePos", BlockPos.CODEC, relPos);
        } else if (optionalRel.isPresent()) {
            Integer relX = optionalRel.get();
            Integer relY = readView.getOptionalInt("RelY").get();
            Integer relZ = readView.getOptionalInt("RelZ").get();
            writeView.put("RelativePos", BlockPos.CODEC, new BlockPos(relX, relY, relZ));
        }
    }
}
