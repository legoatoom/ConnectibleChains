/*
 * Copyright (C) 2021 legoatoom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains;


import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import net.fabricmc.api.ModInitializer;

/**
 * Mod Initializer for Connectible chains.
 */
public class ConnectibleChains implements ModInitializer {

    public static final String MODID = "connectiblechains";

    @Override
    public void onInitialize() {
        ModEntityTypes.init();
    }

}
