package com.github.legoatoom.connectiblechains.compat;

import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.resource.ResourceManager;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ChainTypes implements SimpleSynchronousResourceReloadListener {

    @Override
    public Identifier getFabricId() {
        return Helper.identifier("chain_types");
    }

    @Override
    public void reload(ResourceManager manager) {
        // ChainTypesRegistry.CHAINABLE_TAG does not work, it's updated too late, and I don't know why.
        // getFabricDependencies does not help
        Tag<Item> tag = ServerTagManagerHolder.getTagManager()
                .getOrCreateTagGroup(Registry.ITEM_KEY)
                .getTagOrEmpty(ChainTypesRegistry.CHAINABLE_TAG_ID);
        for (Item item : tag.values()) {
            ChainTypesRegistry.registerDynamic(item);
        }
    }
}

///**
// * Acts like a registry that loads itself.
// * Loads all chain types from the <code>connectiblechains:chainable</code> tag.
// *
// * @author Qendolin
// */
//public class ChainTypes implements SidedResourceReloadListener<List<ChainType>> {
//    public static final Tag<Item> CHAINABLE_CHAINS = TagFactory.ITEM.create(new Identifier(ConnectibleChains.MODID, "chainable"));
//    private static final Gson GSON = new GsonBuilder().setLenient().create();
//    private static final Identifier DEFAULT_TYPE = new Identifier("minecraft:chain");
//    private static final Identifier MISSING_ID = new Identifier(ConnectibleChains.MODID, "textures/entity/missing.png");
//    private final List<Identifier> builtinTypes = loadBuiltinTypes();
//
//    // Identifiers are more performant than Items (I'm pretty sure)
//    private final Map<Identifier, ChainType> registeredTypes = new HashMap<>();
//    private final Set<Identifier> registeredItems = new HashSet<>();
//
//    public ChainType getDefaultType() {
//        return registeredTypes.get(DEFAULT_TYPE);
//    }
//
//    @SuppressWarnings("unused")
//    @Override
//    public CompletableFuture<List<ChainType>> load(ResourceType type, ResourceManager manager, Profiler profiler, Executor executor) {
//        return CompletableFuture.supplyAsync(() -> loadChainTypes(type, manager));
//    }
//
//    /**
//     * Load chain types from builtin and tag, for client and server.
//     *
//     * @param type    The chain type
//     * @param manager The resource manager
//     */
//    private List<ChainType> loadChainTypes(ResourceType type, ResourceManager manager) {
//        List<ChainType> chainTypes = new ArrayList<>();
//        Map<String, List<Identifier>> typesByNamespace = new HashMap<>();
//        for (Identifier id : builtinTypes) {
//            typesByNamespace.putIfAbsent(id.getNamespace(), new ArrayList<>());
//            typesByNamespace.get(id.getNamespace()).add(id);
//        }
//        for (Item item : CHAINABLE_CHAINS.values()) {
//            Identifier id = Registry.ITEM.getId(item);
//            typesByNamespace.putIfAbsent(id.getNamespace(), new ArrayList<>());
//            typesByNamespace.get(id.getNamespace()).add(id);
//        }
//
//        for (Map.Entry<String, List<Identifier>> entry : typesByNamespace.entrySet()) {
//            TextureMap textureMap = null;
//            if (type == ResourceType.CLIENT_RESOURCES) {
//                textureMap = loadTextureMap(manager, entry.getKey());
//            }
//
//            for (Identifier id : entry.getValue()) {
//                chainTypes.add(createChainType(type, manager, id, textureMap));
//            }
//        }
//
//        return chainTypes;
//    }
//
//    /**
//     * Loads all texture maps for a given namespace and merges them with priority ordering.
//     *
//     * @param manager   The resource manager
//     * @param namespace The namespace of the texture map
//     * @return The combined texture map
//     */
//    private TextureMap loadTextureMap(ResourceManager manager, String namespace) {
//        Identifier id = new Identifier(namespace, "textures/entity/connectible_chains_compat.json");
//        List<Resource> resources;
//        try {
//            resources = manager.getAllResources(id);
//        } catch (FileNotFoundException ignored) {
//            ConnectibleChains.LOGGER.error("Missing texture map for {}", namespace);
//            return null;
//        } catch (IOException e) {
//            ConnectibleChains.LOGGER.error("Failed to load texture maps for {}: ", id, e);
//            return null;
//        }
//
//        TextureMap mergedMap = new TextureMap();
//        for (Resource resource : resources) {
//            try (resource) {
//                InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
//                TextureMap map = GSON.fromJson(reader, TextureMap.class);
//                mergedMap.textures.putAll(map.textures);
//            } catch (Exception e) {
//                ConnectibleChains.LOGGER.error("Failed to load texture map for {}: ", resource.getId(), e);
//            }
//        }
//
//        mergedMap.textures.forEach((item, entry) -> {
//            if (entry.chain != null) entry.chain += ".png";
//            if (entry.knot != null) entry.knot += ".png";
//        });
//
//        return mergedMap;
//    }
//
//    /**
//     * Creates the chain type for server and client
//     *
//     * @param type       Server or client resources
//     * @param manager    The resource manager
//     * @param item       The chain type item
//     * @param textureMap The item's namespace texture map
//     * @return A server side chain type when loading {@link ResourceType#SERVER_DATA} or a client side chain type
//     * with the resolved textures.
//     */
//    private ChainType createChainType(ResourceType type, ResourceManager manager, Identifier item, TextureMap textureMap) {
//        if (type == ResourceType.SERVER_DATA) {
//            return create(item);
//        }
//
//        if (textureMap == null) {
//            return create(item, MISSING_ID, MISSING_ID);
//        }
//        TextureMap.Entry textureEntry = textureMap.get(item);
//        if (textureEntry == null) {
//            ConnectibleChains.LOGGER.error("Missing texture entry for {}", item);
//            return create(item, MISSING_ID, MISSING_ID);
//        }
//
//        Identifier chainTexture = Identifier.tryParse(textureEntry.chain);
//        Identifier knotTexture = Identifier.tryParse(textureEntry.knot);
//
//        if (chainTexture == null || !manager.containsResource(chainTexture)) {
//            ConnectibleChains.LOGGER.error("'chain' texture {} for {} not found", chainTexture, item);
//            chainTexture = MISSING_ID;
//        }
//        if (knotTexture == null || !manager.containsResource(knotTexture)) {
//            ConnectibleChains.LOGGER.error("'knot' texture {} for {} not found", chainTexture, item);
//            knotTexture = MISSING_ID;
//        }
//        return create(item, chainTexture, knotTexture);
//    }
//
//    public ChainType create(Identifier item) {
//        return new ChainType(item, null, null);
//    }
//
//    @Environment(EnvType.CLIENT)
//    public ChainType create(Identifier id, Identifier texture, Identifier knotTexture) {
//        return create(id, texture, knotTexture);
//    }
//
//    @SuppressWarnings("unused")
//    @Override
//    public CompletableFuture<Void> apply(ResourceType type, List<ChainType> chainTypes, ResourceManager manager, Profiler profiler, Executor executor) {
//        MinecraftClient instance = MinecraftClient.getInstance();
//        // FIXME: Since ChainType's are referenced directly it is not possible to update at runtime.
//        // So for now the game has to be restarted when the types change.
//        // Possible fixes:
//        //   - Reference the types by id and look them up when accessing their properties;
//        //   - Make ChainType mutable and update the shared references
//        // A closer look at how minecraft handles resource reloads in registries is required before
//        // choosing a solution.
//        if(!registeredTypes.isEmpty()) {
//            ConnectibleChains.LOGGER.warn("Reloading chain types is currently not supported, please restart your game to apply anychanges.");
//            return CompletableFuture.completedFuture(null);
//        }
//
//        registeredTypes.clear();
//        registeredItems.clear();
//        for (ChainType chainType : chainTypes) {
//            registeredTypes.put(chainType.item(), chainType);
//            registeredItems.add(chainType.item());
//        }
//
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @SuppressWarnings("unused")
//    @Override
//    public Identifier getFabricId() {
//        return new Identifier(ConnectibleChains.MODID, "chain_types");
//    }
//
//    public boolean has(Item item) {
//        return registeredItems.contains(Registry.ITEM.getId(item));
//    }
//
//    public ChainType get(Item item) {
//        return registeredTypes.get(Registry.ITEM.getId(item));
//    }
//
//    @SuppressWarnings("unused")
//    public ChainType getOrDefault(Item item) {
//        return registeredTypes.getOrDefault(Registry.ITEM.getId(item), registeredTypes.get(DEFAULT_TYPE));
//    }
//
//    @SuppressWarnings("unused")
//    public ChainType get(Identifier id) {
//        return registeredTypes.get(id);
//    }
//
//    public ChainType getOrDefault(Identifier id) {
//        return registeredTypes.getOrDefault(id, registeredTypes.get(DEFAULT_TYPE));
//    }
//
//    @SuppressWarnings("unused")
//    public ChainType get(int id) {
//        return registeredTypes.get(Registry.ITEM.getId(Registry.ITEM.get(id)));
//    }
//
//    public ChainType getOrDefault(int id) {
//        return registeredTypes.getOrDefault(Registry.ITEM.getId(Registry.ITEM.get(id)), registeredTypes.get(DEFAULT_TYPE));
//    }
//
//    /**
//     * These types ship with the mod by default but will not be maintained.
//     * See issue <a href="https://github.com/legoatoom/ConnectibleChains/issues/13">#13</a>.
//     */
//    private List<Identifier> loadBuiltinTypes() {
//        List<Identifier> builtin = new ArrayList<>();
//
//        builtin.add(new Identifier("minecraft:chain"));
//
//        // If the ids change use 'if' + 'else if' and check the newer version first
//        if (isVersionLoaded("betterend", "0.9.0")) {
//            builtin.add(new Identifier("betterend:thallasium_chain"));
//            builtin.add(new Identifier("betterend:terminite_chain"));
//        }
//        if (isVersionLoaded("betternether", "3.5.0")) {
//            builtin.add(new Identifier("betternether:cincinnasite_chain"));
//        }
//        if (isVersionLoaded("valley", "2.0")) {
//            builtin.add(new Identifier("valley:golden_chain"));
//            builtin.add(new Identifier("valley:netherite_chain"));
//            builtin.add(new Identifier("valley:copper_chain"));
//        }
//
//        return builtin;
//    }
//
//    /**
//     * Checks if a mod is loaded, and it's version is above or equal to version
//     *
//     * @param modid   The mod id from fabric.mod.json
//     * @param version A SemVer version
//     * @return true if the mod with the version is loaded
//     */
//    public boolean isVersionLoaded(String modid, String version) {
//        // This method is quite useless now, but I like it, so I kept it :<| .
//        FabricLoader loader = FabricLoader.getInstance();
//        if (!loader.isModLoaded(modid)) return false;
//        Optional<ModContainer> modContainer = loader.getModContainer(modid);
//        if (modContainer.isEmpty()) {
//            ConnectibleChains.LOGGER.info("Mod {} loaded but no container present", modid);
//            return false;
//        }
//        String givenVersion = modContainer.get().getMetadata().getVersion().getFriendlyString();
//        return isVersionAbove(version, givenVersion);
//    }
//
//    /**
//     * Accepts to SemVer formatted versions and checks if {@code want} >= {@code have}
//     *
//     * @param want The minimum version
//     * @param have The actual version
//     * @return true when {@code want} >= {@code have}
//     */
//    private static boolean isVersionAbove(String want, String have) {
//        // remove version meta info
//        want = want.replaceAll("[+-].*", "");
//        have = have.replaceAll("[+-].*", "");
//        List<String> wantParts = Arrays.asList(want.split("\\."));
//        List<String> haveParts = Arrays.asList(have.split("\\."));
//
//        // ensure same size
//        while (wantParts.size() < haveParts.size()) wantParts.add("0");
//        while (haveParts.size() < wantParts.size()) haveParts.add("0");
//
//        for (int i = 0; i < wantParts.size(); i++) {
//            try {
//                int haveInt = Integer.parseInt(haveParts.get(i));
//                int wantInt = Integer.parseInt(wantParts.get(i));
//
//                if (haveInt > wantInt) return true;
//                if (haveInt < wantInt) return false;
//            } catch (NumberFormatException e) {
//                ConnectibleChains.LOGGER.warn("Bad formatted version {} or {}", want, have);
//            }
//        }
//        // want == have
//        return true;
//    }
//}
