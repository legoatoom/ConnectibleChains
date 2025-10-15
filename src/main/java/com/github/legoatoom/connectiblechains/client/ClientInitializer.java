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

package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.networking.packet.ChainAttachS2CPacket;
import com.github.legoatoom.connectiblechains.networking.packet.ConfigSyncPayload;
import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainCollisionEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.ChainKnotEntityRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.CatenaryRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.CrossCatenaryRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.PlussCatenaryRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.catenary.SquareCatenaryRenderer;
import com.github.legoatoom.connectiblechains.client.render.entity.model.ChainKnotEntityModel;
import com.github.legoatoom.connectiblechains.client.render.entity.texture.ChainModelReloader;
import com.github.legoatoom.connectiblechains.config.ModConfig;
import com.github.legoatoom.connectiblechains.entity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.item.ChainItemCallbacks;
import com.github.legoatoom.connectiblechains.util.Helper;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactories;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;

import java.util.Optional;

import static net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver;

/**
 * ClientInitializer.
 * This method is called when the game starts with a client.
 * This registers the renderers for entities and how to handle packages between the server and client.
 *
 * @author legoatoom
 */
@Environment(EnvType.CLIENT)
public class ClientInitializer implements ClientModInitializer {

    public static final EntityModelLayer CHAIN_KNOT = new EntityModelLayer(Helper.identifier("chain_knot"), "main");
    private static ClientInitializer instance;
    private final ChainModelReloader chainModelReloader = new ChainModelReloader();
    private ChainKnotEntityRenderer chainKnotEntityRenderer;


    @Override
    public void onInitializeClient() {
        instance = this;
        initRenderers();

        registerNetworkEventHandlers();
        registerClientEventHandlers();

        registerConfigSync();

        // Tooltip for chains.
        ItemTooltipCallback.EVENT.register(ChainItemCallbacks::infoToolTip);

        registerCatenaryRenders();
    }

    private void registerCatenaryRenders() {
        CatenaryRenderer.addRenderer(Helper.identifier("cross"), CrossCatenaryRenderer::new);
        CatenaryRenderer.addRenderer(Helper.identifier("square"), SquareCatenaryRenderer::new);
        CatenaryRenderer.addRenderer(Helper.identifier("plus"), PlussCatenaryRenderer::new);
    }

    private static void registerConfigSync() {
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
    }

    private void initRenderers() {
        ConnectibleChains.LOGGER.info("Initializing Renderers.");
        EntityRendererFactories.register(ModEntityTypes.CHAIN_KNOT, ctx -> {
            chainKnotEntityRenderer = new ChainKnotEntityRenderer(ctx);
            return chainKnotEntityRenderer;
        });
        EntityRendererFactories.register(ModEntityTypes.CHAIN_COLLISION, ChainCollisionEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(CHAIN_KNOT, ChainKnotEntityModel::getTexturedModelData);
    }

    private void registerNetworkEventHandlers() {
        ConnectibleChains.LOGGER.info("Initializing Network even handlers.");

        registerGlobalReceiver(ChainAttachS2CPacket.PAYLOAD_ID, ChainAttachS2CPacket::apply);

        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            // Load client config
            ConnectibleChains.runtimeConfig.copyFrom(ConnectibleChains.fileConfig);
            getChainKnotEntityRenderer().ifPresent(r -> r.getChainRenderer().purge());
        });

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.PAYLOAD_ID, ConfigSyncPayload::apply);
    }

    private void registerClientEventHandlers() {
        ConnectibleChains.LOGGER.info("Registering texture handlers..");

        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(Helper.identifier("chain_model_reloader"), chainModelReloader);
    }

    public static ClientInitializer getInstance() {
        return instance;
    }

    public Optional<ChainKnotEntityRenderer> getChainKnotEntityRenderer() {
        return Optional.ofNullable(chainKnotEntityRenderer);
    }

    public ChainModelReloader getChainTextureManager() {
        return chainModelReloader;
    }
}
