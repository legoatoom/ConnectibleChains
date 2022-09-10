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

package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainTextureManager;
import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.config.ModConfig;
import com.github.legoatoom.connectiblechains.enitity.ChainCollisionEntity;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Optional;

/**
 * ClientInitializer.
 * This method is called when the game starts with a client.
 * This registers the renderers for entities and how to handle packages between the server and client.
 *
 * @author legoatoom
 */
public class ClientInitializer implements ClientModInitializer {

    public static final EntityModelLayer CHAIN_KNOT = new EntityModelLayer(Helper.identifier("chain_knot"), "main");
    private static ClientInitializer instance;
    public final ChainTextureManager textureManager = new ChainTextureManager();
    private ChainKnotEntityRenderer chainKnotEntityRenderer;
    private ChainPacketHandler chainPacketHandler;

    @Override
    public void onInitializeClient() {
        instance = this;
        initRenderers();

        registerNetworkEventHandlers();
        registerClientEventHandlers();

        ConfigHolder<ModConfig> configHolder = AutoConfig.getConfigHolder(ModConfig.class);
        configHolder.registerSaveListener((holder, modConfig) -> {
            ClientInitializer clientInitializer = ClientInitializer.getInstance();
            if (clientInitializer != null) {
                clientInitializer.getChainKnotEntityRenderer().ifPresent(renderer -> renderer.getChainRenderer().purge());
            }
            MinecraftServer server = MinecraftClient.getInstance().getServer();
            if (server != null) {
                ConnectibleChains.LOGGER.info("Syncing config to clients");
                ConnectibleChains.fileConfig.syncToClients(server);
                ConnectibleChains.runtimeConfig.copyFrom(ConnectibleChains.fileConfig);
            }
            return ActionResult.PASS;
        });

        // Tooltip for chains.
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (ChainTypesRegistry.ITEM_CHAIN_TYPES.containsKey(stack.getItem())){
                lines.add(1, MutableText.of(new TranslatableTextContent("message.connectiblechains.connectible_chain")).formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
            }
        });
    }

    private void initRenderers() {
        ConnectibleChains.LOGGER.info("Initializing Renderers.");
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_KNOT, ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
        EntityRendererRegistry.register(ModEntityTypes.CHAIN_COLLISION,
                ChainCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    private void registerNetworkEventHandlers() {
        chainPacketHandler = new ChainPacketHandler();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Load client config
            ConnectibleChains.runtimeConfig.copyFrom(ConnectibleChains.fileConfig);
            getChainKnotEntityRenderer().ifPresent(r->r.getChainRenderer().purge());
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkingPackets.S2C_CONFIG_SYNC_PACKET,
                (client, handler, packetByteBuf, responseSender) -> {
                    // Apply server config
                    if (client.isInSingleplayer()) {
                        return;
                    }
                    try {
                        ConnectibleChains.LOGGER.info("Received {} config from server", ConnectibleChains.MODID);
                        ConnectibleChains.runtimeConfig.readPacket(packetByteBuf);
                    } catch (Exception e) {
                        ConnectibleChains.LOGGER.error("Could not deserialize config: ", e);
                    }
                    getChainKnotEntityRenderer().ifPresent(renderer -> renderer.getChainRenderer().purge());
                });
    }

    private void registerClientEventHandlers() {
        ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
            if (result instanceof EntityHitResult) {
                Entity entity = ((EntityHitResult) result).getEntity();
                if (entity instanceof ChainKnotEntity knot) {
                    return new ItemStack(knot.getChainType().item());
                } else if (entity instanceof ChainCollisionEntity collision) {
                    return new ItemStack(collision.getChainType().item());
                }
            }
            return ItemStack.EMPTY;
        });

        ClientTickEvents.START_WORLD_TICK.register(world -> chainPacketHandler.tick());

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(textureManager);
        // ServerInitializer uses SERVER_STARTED
        ClientLifecycleEvents.CLIENT_STARTED.register((client) -> ChainTypesRegistry.lock());
    }

    public static ClientInitializer getInstance() {
        return instance;
    }

    private Optional<ChainKnotEntityRenderer> getChainKnotEntityRenderer() {
        return Optional.ofNullable(chainKnotEntityRenderer);
    }
}
