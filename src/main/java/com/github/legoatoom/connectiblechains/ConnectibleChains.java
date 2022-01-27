/*
 * Copyright (C) 2022 legoatoom
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
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains;


import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.compat.ChainTypes;
import com.github.legoatoom.connectiblechains.compat.SidedResourceReloadListener;
import com.github.legoatoom.connectiblechains.config.ModConfig;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ChainLinkEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Mod Initializer for Connectible chains.
 */
public class ConnectibleChains implements ModInitializer {

    /**
     * All mods need to have an ID, that is what tells the game and fabric what each mod is.
     * These need to be unique for all mods, and always stay the same in your mod, so by creating a field
     * it will be a lot easier!
     */
    public static final String MODID = "connectiblechains";
    public static final ChainTypes TYPES = new ChainTypes();
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    /**
     * ModConfigs are helpful if people keep demanding for your chains to get longer...
     * File config is what's saved on disk, runtimeConfig should be used in most cases
     */
    public static ModConfig fileConfig;
    /**
     * Runtime config is a mix of the client and server config and should not be saved to disk
     */
    public static ModConfig runtimeConfig;

    /**
     * Here is where the fun begins.
     */
    @Override
    public void onInitialize() {
        ModEntityTypes.init();

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(
                SidedResourceReloadListener.proxy(ResourceType.SERVER_DATA, TYPES));

        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        fileConfig = configHolder.getConfig();
        runtimeConfig = new ModConfig().copyFrom(fileConfig);

        UseBlockCallback.EVENT.register(ConnectibleChains::chainUseEvent);

        ServerPlayConnectionEvents.INIT.register((handler, server) -> fileConfig.syncToClient(handler.getPlayer()));
    }

    /**
     * Because of how mods work, this function is called always when a player uses right click.
     * But if the right click doesn't involve this mod (No chain/block to connect to) then we ignore immediately.
     * <p>
     * If it does involve us, then we have work to do, we create connections remove items from inventory and such.
     *
     * @param player    PlayerEntity that right-clicked on a block.
     * @param world     The world the player is in.
     * @param hand      What hand the player used.
     * @param hitResult General information about the block that was clicked.
     * @return An ActionResult.
     */
    private static ActionResult chainUseEvent(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (player == null || player.isSneaking()) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        BlockPos blockPos = hitResult.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();

        if (!ChainKnotEntity.canAttachTo(block)) return ActionResult.PASS;
        else if (world.isClient) {
            Item handItem = player.getStackInHand(hand).getItem();
            if (ConnectibleChains.TYPES.has(handItem)) {
                return ActionResult.SUCCESS;
            }

            // Check if any held chains can be attached. This can be done without holding a chain item
            if (ChainKnotEntity.getHeldChainsInRange(player, blockPos).size() > 0) {
                return ActionResult.SUCCESS;
            }

            // Check if a knot exists and can be destroyed
            // Would work without this check but no swing animation would be played
            if (ChainKnotEntity.getKnotAt(player.world, blockPos) != null && ChainLinkEntity.canDestroyWith(item)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        // 1. Try with existing knot, regardless of hand item
        ChainKnotEntity knot = ChainKnotEntity.getKnotAt(world, blockPos);
        if (knot != null) {
            if (knot.interact(player, hand) == ActionResult.CONSUME) {
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }

        // 2. Check if any held chains can be attached.
        List<ChainLink> attachableChains = ChainKnotEntity.getHeldChainsInRange(player, blockPos);

        // Use the held item as the new knot type
        ChainType knotType = ConnectibleChains.TYPES.get(item);

        // Allow default interaction behaviour.
        if (attachableChains.size() == 0 && knotType == null) return ActionResult.PASS;

        // Held item does not correspond to a type.
        if (knotType == null)
            knotType = attachableChains.get(0).chainType;

        // 2. Create new knot if none exists and delegate interaction
        knot = new ChainKnotEntity(world, blockPos, knotType);
        knot.setGraceTicks((byte) 0);
        world.spawnEntity(knot);
        knot.onPlace();
        return knot.interact(player, hand);
    }

}
