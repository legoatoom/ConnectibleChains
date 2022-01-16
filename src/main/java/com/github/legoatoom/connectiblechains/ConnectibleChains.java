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


import com.github.legoatoom.connectiblechains.client.ClientInitializer;
import com.github.legoatoom.connectiblechains.config.ModConfig;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
        if (player == null) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        BlockPos blockPos = hitResult.getBlockPos();
        Block block = world.getBlockState(blockPos).getBlock();
        if (stack.getItem() == Items.CHAIN) {
            if (ChainKnotEntity.canConnectTo(block) && !player.isSneaking()) {
                if (!world.isClient) {
                    ChainKnotEntity knot = ChainKnotEntity.getOrCreate(world, blockPos, false);
                    if (!ChainKnotEntity.tryAttachHeldChainsToBlock(player, world, blockPos, knot)) {
                        // If this didn't work connect the player to the new chain instead.
                        assert knot != null; // This can never happen as long as getOrCreate has false as parameter.
                        if (knot.getHoldingEntities().contains(player)) {
                            knot.detachChain(player, true, false);
                            knot.onBreak(null);
                            if (!player.isCreative())
                                stack.increment(1);
                        } else if (knot.attachChain(player, true, 0)) {
                            knot.onPlace();
                            if (!player.isCreative())
                                stack.decrement(1);
                        }
                    }
                }
                return ActionResult.success(world.isClient);
            }
        }
        if (ChainKnotEntity.canConnectTo(block)) {
            if (world.isClient) {
                ItemStack itemStack = player.getStackInHand(hand);
                return itemStack.getItem() == Items.CHAIN ? ActionResult.SUCCESS : ActionResult.PASS;
            } else {
                return ChainKnotEntity.tryAttachHeldChainsToBlock(player, world, blockPos, ChainKnotEntity.getOrCreate(world, blockPos, true)) ? ActionResult.SUCCESS : ActionResult.PASS;
            }
        }
        return ActionResult.PASS;
    }

    /**
     * Here is where the fun begins.
     */
    @Override
    public void onInitialize() {
        ModEntityTypes.init();
        AutoConfig.register(ModConfig.class, Toml4jConfigSerializer::new);
        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        fileConfig = configHolder.getConfig();
        runtimeConfig = new ModConfig().copyFrom(fileConfig);

        UseBlockCallback.EVENT.register(ConnectibleChains::chainUseEvent);

        ServerPlayConnectionEvents.INIT.register((handler, server) -> fileConfig.syncToClient(handler.getPlayer()));
    }

}
