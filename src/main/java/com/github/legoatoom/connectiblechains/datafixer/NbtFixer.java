package com.github.legoatoom.connectiblechains.datafixer;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.*;

/**
 * Little utility to upgrade NBT data to newer versions of the mod.
 * This is allows the mod to be updated without breaking existing worlds.
 *
 * @implNote Only entity nbt fixes are supported.
 * See {@code NbtHelperMixin#updateDataWithFixers} to add support for more.
 * @apiNote {@link #update(NbtCompound)} and {@link #addVersionTag(NbtCompound)} have to be called manually.
 */
public abstract class NbtFixer {
    /**
     * Key for the nbt value that holds the data version.
     */
    private static final String DATA_VERSION_KEY = ConnectibleChains.MODID + "_DataVersion";
    /**
     * A map of fixes sorted by their version number.
     * The sorting ensures that the fixes are applied from oldest to newest.
     */
    private final SortedMap<Integer, List<NamedFix>> fixes = new TreeMap<>();

    public NbtFixer() {
        registerFixers();
    }

    public abstract void registerFixers();

    /**
     * Applies all fixes between the stored and the current version.
     *
     * @param nbt The <a href="https://minecraft.fandom.com/wiki/Entity_format#Entity_Format">ENTITY_CHUNK</a> nbt data to be fixed.
     * @return The fixed NBT data
     */
    public NbtCompound update(NbtCompound nbt) {
        NbtList entities = nbt.getList("Entities", NbtType.COMPOUND);
        for (NbtElement entity : entities) {
            updateEntity((NbtCompound) entity);
        }
        return nbt;
    }

    protected void updateEntity(NbtCompound nbt) {
        int currentVersion = nbt.getInt(DATA_VERSION_KEY);
        if (currentVersion >= getVersion()) return;
        // Only update entities from this mod
        if (!nbt.getString("id").startsWith(ConnectibleChains.MODID)) return;
        for (Map.Entry<Integer, List<NamedFix>> entry : fixes.entrySet()) {
            if (entry.getKey() <= currentVersion) continue;
            for (NamedFix namedFix : entry.getValue()) {
                try {
                    nbt = namedFix.fix.apply(nbt);
                } catch (Exception e) {
                    ConnectibleChains.LOGGER.error("During fix '{}' for '{}': ", namedFix.name, nbt, e);
                }
            }
        }
    }

    /**
     * Returns the current data format version.
     * The lowest two digits are for the patch level.
     * The next two are for minor versions.
     * The rest are fore major versions.
     */
    protected abstract int getVersion();

    /**
     * Adds the data version nbt tag to {@code nbt}
     */
    public void addVersionTag(NbtCompound nbt) {
        nbt.putInt(DATA_VERSION_KEY, getVersion());
    }

    /**
     * Adds a fix for a specific version
     *
     * @param version The minimum version for the fix
     * @param name    A descriptive name
     * @param fix     The fixer
     */
    @SuppressWarnings("SameParameterValue")
    protected void addFix(int version, String name, Fix fix) {
        if (!fixes.containsKey(version)) fixes.put(version, new ArrayList<>());
        fixes.get(version).add(new NamedFix(name, fix));
    }

    interface Fix {
        NbtCompound apply(NbtCompound nbt);
    }

    private record NamedFix(String name, Fix fix) {
    }
}
