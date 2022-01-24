package com.github.legoatoom.connectiblechains.compat;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.chain.UV;
import com.github.legoatoom.connectiblechains.mixin.client.JsonUnbakedModelAccessor;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.TagFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.Item;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
public class ChainTypes implements SidedResourceReloadListener<List<ChainType>> {
    public static final Tag<Item> CONNECTIBLE_CHAINS = TagFactory.ITEM.create(new Identifier(ConnectibleChains.MODID, "chainable"));
    private static final Identifier DEFAULT_TYPE = new Identifier("minecraft:chain");
    private static final Identifier MISSING_ID = new Identifier("missing");


    private final List<Identifier> builtinTypes = loadBuiltinTypes();

    // Identifiers are more performant than Items (I'm pretty sure)
    private final Map<Identifier, ChainType> registeredTypes = new HashMap<>();
    private final Set<Identifier> registeredItems = new HashSet<>();


    @Environment(EnvType.CLIENT)
    public ChainType create(Identifier item, Identifier texture, Identifier knotTexture, UV uvSideA, UV uvSideB) {
        return new ChainType(item, texture, knotTexture, uvSideA, uvSideB);
    }

    public ChainType create(Identifier item) {
        return new ChainType(item, null, null, null, null);
    }

    @Environment(EnvType.CLIENT)
    public ChainType create(Identifier id, Identifier texture, Identifier knotTexture) {
        return create(id, texture, knotTexture, UV.DEFAULT_SIDE_A, UV.DEFAULT_SIDE_B);
    }

    public boolean has(Item item) {
        return registeredItems.contains(Registry.ITEM.getId(item));
    }

    public ChainType get(Item item) {
        return registeredTypes.get(Registry.ITEM.getId(item));
    }

    @SuppressWarnings("unused")
    public ChainType get(Identifier id) {
        return registeredTypes.get(id);
    }

    public ChainType getOrDefault(Identifier id) {
        return registeredTypes.getOrDefault(id, registeredTypes.get(DEFAULT_TYPE));
    }

    public ChainType get(int id) {
        return registeredTypes.get(Registry.ITEM.getId(Registry.ITEM.get(id)));
    }

    public ChainType getOrDefault(int id) {
        return registeredTypes.getOrDefault(Registry.ITEM.getId(Registry.ITEM.get(id)), registeredTypes.get(DEFAULT_TYPE));
    }

    public ChainType getDefaultType() {
        return registeredTypes.get(DEFAULT_TYPE);
    }

    /**
     * Checks if a mod is loaded, and it's version is above or equal to version
     * @param modid The mod id from fabric.mod.json
     * @param version A SemVer version
     * @return true if the mod with the version is loaded
     */
    public boolean isVersionLoaded(String modid, String version) {
        FabricLoader loader = FabricLoader.getInstance();
        if(!loader.isModLoaded(modid)) return false;
        Optional<ModContainer> modContainer = loader.getModContainer(modid);
        if(modContainer.isEmpty()) {
            ConnectibleChains.LOGGER.info("Mod {} loaded but no container present", modid);
            return false;
        }
        String givenVersion = modContainer.get().getMetadata().getVersion().getFriendlyString();
        return isVersionAbove(version, givenVersion);
    }

    private static boolean isVersionAbove(String want, String have) {
        // remove version meta info
        want = want.replaceAll("[+-].*", "");
        have = have.replaceAll("[+-].*", "");
        List<String> wantParts = Arrays.asList(want.split("\\."));
        List<String> haveParts = Arrays.asList(have.split("\\."));

        while (wantParts.size() < haveParts.size()) wantParts.add("0");
        while (haveParts.size() < wantParts.size()) haveParts.add("0");

        for (int i = 0; i < wantParts.size(); i++) {
            try {
                int haveInt = Integer.parseInt(haveParts.get(i));
                int wantInt = Integer.parseInt(wantParts.get(i));

                if (haveInt > wantInt) return true;
                if (haveInt < wantInt) return false;
            } catch (NumberFormatException e) {
                ConnectibleChains.LOGGER.warn("Bad formatted version {} or {}", want, have);
            }
        }
        // want == have
        return true;
    }

    private List<Identifier> loadBuiltinTypes() {
        List<Identifier> builtin = new ArrayList<>();

        builtin.add(new Identifier("minecraft:chain"));

        // If the ids change use 'if' + 'else if' and check the newer version first
        if (isVersionLoaded("betterend", "0.9.0")) {
            builtin.add(new Identifier("betterend:thallasium_chain"));
            builtin.add(new Identifier("betterend:terminite_chain"));
        }
        if (isVersionLoaded("betternether", "3.5.0")) {
            builtin.add(new Identifier("betternether:cincinnasite_chain"));
        }
        if (isVersionLoaded("valley", "2.0")) {
            builtin.add(new Identifier("valley:golden_chain"));
            builtin.add(new Identifier("valley:netherite_chain"));
            builtin.add(new Identifier("valley:copper_chain"));
        }

        return builtin;
    }

    @Override
    public CompletableFuture<List<ChainType>> load(ResourceType type, ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> loadChainTypes(type, manager));
    }

    @Override
    public CompletableFuture<Void> apply(ResourceType type, List<ChainType> chainTypes, ResourceManager manager, Profiler profiler, Executor executor) {
        registeredTypes.clear();
        registeredItems.clear();
        for (ChainType chainType : chainTypes) {
            registeredTypes.put(chainType.item(), chainType);
            registeredItems.add(chainType.item());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Register chain types from builtin and tag, for client and server.
     * @param type The chain type
     * @param manager The resouce manager
     */
    private List<ChainType> loadChainTypes(ResourceType type, ResourceManager manager) {
        List<ChainType> chainTypes = new ArrayList<>();
        for (Identifier item : builtinTypes) {
            chainTypes.add(loadChainType(type, manager, item));
        }
        for (Item item : CONNECTIBLE_CHAINS.values()) {
            chainTypes.add(loadChainType(type, manager, Registry.ITEM.getId(item)));
        }
        return chainTypes;
    }

    private ChainType loadChainType(ResourceType type, ResourceManager manager, Identifier item) {
        if(type == ResourceType.SERVER_DATA) {
            return create(item);
        }

        String name = item.toUnderscoreSeparatedString();

        Identifier modelId = Helper.identifier("models/entity/" + name + ".json");
        Map<String, Identifier> textureMap = loadTextureMap(manager, modelId);

        if(textureMap == null) {
            ConnectibleChains.LOGGER.error("Missing model {} for {}", modelId, item);
            return create(item, MISSING_ID, MISSING_ID);
        }

        Identifier textureId = textureMap.get("chain");
        Identifier knotTextureId = textureMap.get("chain_knot");

        if(textureId == null) {
            ConnectibleChains.LOGGER.error("Missing 'chain' texture for {}", item);
            textureId = MISSING_ID;
        }
        if(knotTextureId == null) {
            ConnectibleChains.LOGGER.error("Missing 'chain_knot' texture for {}", item);
            textureId = MISSING_ID;
        }
        return create(item, textureId, knotTextureId);
    }

    private Map<String, Identifier> loadTextureMap(ResourceManager manager, Identifier id) {
        Resource resource;
        try {
            resource = manager.getResource(id);
        } catch (IOException e) {
            return null;
        }
        InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
        JsonUnbakedModel model = JsonUnbakedModel.deserialize(reader);
        IOUtils.closeQuietly(reader, resource);

        Map<String, Either<SpriteIdentifier, String>> textureMap = ((JsonUnbakedModelAccessor) model).getTextureMap();
        Map<String, Identifier> result = new HashMap<>();
        textureMap.forEach((key, value) -> {
            if(value.left().isPresent()) result.put(key, new Identifier(value.left().get().getTextureId() + ".png"));
        });

        return result;
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier(ConnectibleChains.MODID, "chain_types");
    }
}
