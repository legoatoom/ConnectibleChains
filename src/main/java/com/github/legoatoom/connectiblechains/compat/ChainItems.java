package com.github.legoatoom.connectiblechains.compat;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.*;

public class ChainItems {
    // Identifiers are more performant than Items (I'm pretty sure)
    private static final Map<Identifier, Type> TYPES = new HashMap<>();
    private static final Set<Identifier> ITEMS = new HashSet<>();

    public static void addType(String itemId, String texture, UV uvSideA, UV uvSideB) {
        Identifier itemIdentifier = new Identifier(itemId);
        TYPES.put(itemIdentifier, new Type(itemIdentifier, new Identifier(texture), uvSideA, uvSideB));
        ITEMS.add(itemIdentifier);
    }

    public static void addType(String id, String texture) {
        addType(id, texture, UV.DEFAULT_SIDE_A, UV.DEFAULT_SIDE_B);
    }

    public static boolean has(Item item) {
        return ITEMS.contains(Registry.ITEM.getId(item));
    }

    public static Type getType(Item item) {
        return TYPES.get(Registry.ITEM.getId(item));
    }

    /**
     * Checks if a mod is loaded and it's version is above or equal to version
     * @param modid The mod id from fabric.mod.json
     * @param version A SemVer version
     * @return true if the mod with the version is loaded
     */
    public static boolean isVersionLoaded(String modid, String version) {
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

    public static void register() {
        addType("minecraft:chain", "minecraft:textures/block/chain.png");

        // If the uvs or ids change use 'if' + 'else if' and check the newer version first

        if(isVersionLoaded("betterend", "0.9.0")) {
            addType("betterend:thallasium_chain", "betterend:textures/block/thallasium_chain.png");
            addType("betterend:terminite_chain", "betterend:textures/block/terminite_chain.png");
        }
        if(isVersionLoaded("betternether", "3.5.0")) {
            addType("betternether:cincinnasite_chain", "betternether:textures/block/cincinnasite_chain.png");
        }
        if(isVersionLoaded("valley", "2.0")) {
            addType("valley:golden_chain", "valley:textures/blocks/golden_chain_block.png");
            addType("valley:netherite_chain", "valley:textures/blocks/netherite_chain_block.png");
            addType("valley:copper_chain", "valley:textures/blocks/copper_chain_block.png");
        }
    }

    // The chain texture has to be vertical for now
    public static record UV(float x0, float x1) {
        public static UV DEFAULT_SIDE_A = new UV(0, 3);
        public static UV DEFAULT_SIDE_B = new UV(3, 6);
    }

    public static record Type(Identifier item, Identifier texture, UV uvSIdeA, UV uvSideB) {}
}
