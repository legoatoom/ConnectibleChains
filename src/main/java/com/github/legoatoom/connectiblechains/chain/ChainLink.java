/*
 * Copyright (C) 2023 legoatoom
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.entity.ChainCollisionEntity;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.entity.ModEntityTypes;
import com.github.legoatoom.connectiblechains.util.Helper;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A logical representation of the link between a knot and another entity.
 * It also serves as a single source of truth which prevents state mismatches in the code.
 *
 * @author Qendolin
 */
public class ChainLink {
    /**
     * The x/z distance between {@link ChainCollisionEntity ChainCollisionEntities}.
     * A value of 1 means they are "shoulder to shoulder"
     */
    private static final float COLLIDER_SPACING = 1.5f;

    /**
     * The de facto owner of this link. It is responsive for managing the link and keeping track of it across saves.
     */
    @NotNull
    public final ChainKnotEntity primary;
    /**
     * The de facto target of this link. Mostly used to calculate positions.
     */
    @NotNull
    public final Entity secondary;
    /**
     * The type of the link
     */
    @NotNull
    public final Item sourceItem;
    /**
     * Holds the entity ids of associated {@link ChainCollisionEntity collision entities}.
     */
    private final IntList collisionStorage = new IntArrayList(16);
    /**
     * Indicates that no sound should be played when the link is destroyed.
     */
    public boolean removeSilently = false;
    /**
     * Whether the link exists and is active
     */
    private boolean alive = true;

    private ChainLink(@NotNull ChainKnotEntity primary, @NotNull Entity secondary, @NotNull Item sourceItem) {
        if (primary.equals(secondary))
            throw new IllegalStateException("Tried to create a link between a knot and itself");
        this.primary = Objects.requireNonNull(primary);
        this.secondary = Objects.requireNonNull(secondary);
        this.sourceItem = Objects.requireNonNull(sourceItem);
    }

    /**
     * Create a chain link between primary and secondary,
     * adds it to their lists. Also spawns {@link ChainCollisionEntity collision entities}
     * when the link is created between two knots.
     *
     * @param primary   The source knot
     * @param secondary A different chain knot or player
     * @param sourceItem The type of the link
     * @return A new chain link or null if it already exists
     */
    @Nullable
    public static ChainLink create(@NotNull ChainKnotEntity primary, @NotNull Entity secondary, @NotNull Item sourceItem) {
        ChainLink link = new ChainLink(primary, secondary, sourceItem);
        // Prevent multiple links between same targets.
        // Checking on the secondary is not required as the link always exists on both sides.
        if (primary.getLinks().contains(link)) return null;

        primary.addLink(link);
        if (secondary instanceof ChainKnotEntity secondaryKnot) {
            secondaryKnot.addLink(link);
            link.createCollision();
        }
        if (!primary.world.isClient) {
            link.sendAttachChainPacket(primary.world);
        }
        return link;
    }

    /**
     * Create a collision between this and an entity.
     * It spawns multiple {@link ChainCollisionEntity ChainCollisionEntities} that are equal distance from each other.
     * Position is the same no matter what if the connection is from A -> B or A <- B.
     */
    private void createCollision() {
        if (!collisionStorage.isEmpty()) return;
        if (primary.world.isClient) return;

        double distance = primary.distanceTo(secondary);
        // step = spacing * √(width^2 + width^2) / distance
        double step = COLLIDER_SPACING * Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.getWidth(), 2) * 2) / distance;
        double v = step;
        // reserve space for the center collider
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.getWidth() / distance;

        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, primary, secondary, v);
            if (collider1 != null) collisionStorage.add(collider1.getId());
            Entity collider2 = spawnCollision(true, primary, secondary, v);
            if (collider2 != null) collisionStorage.add(collider2.getId());

            v += step;
        }

        Entity centerCollider = spawnCollision(false, primary, secondary, 0.5);
        if (centerCollider != null) collisionStorage.add(centerCollider.getId());
    }

    /**
     * Send a package to all the clients around this entity that notifies them of this link's creation.
     */
    private void sendAttachChainPacket(World world) {
        assert world instanceof ServerWorld;

        Set<ServerPlayerEntity> trackingPlayers = getTrackingPlayers(world);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        buf.writeVarInt(primary.getId());
        buf.writeVarInt(secondary.getId());
        buf.writeVarInt(Registries.ITEM.getRawId(sourceItem));

        for (ServerPlayerEntity player : trackingPlayers) {
            ServerPlayNetworking.send(player, NetworkingPackets.S2C_CHAIN_ATTACH_PACKET_ID, buf);
        }
    }

    /**
     * Spawns a collider at {@code v} percent between {@code start} and {@code end}
     *
     * @param reverse Reverse start and end
     * @param start   the entity at {@code v} = 0
     * @param end     the entity at {@code v} = 1
     * @param v       percent of the distance
     * @return {@link ChainCollisionEntity} or null
     */
    @Nullable
    private Entity spawnCollision(boolean reverse, Entity start, Entity end, double v) {
        assert primary.world instanceof ServerWorld;
        Vec3d startPos = start.getPos().add(start.getLeashOffset(0));
        Vec3d endPos = end.getPos().add(end.getLeashOffset(0));

        Vec3d tmp = endPos;
        if (reverse) {
            endPos = startPos;
            startPos = tmp;
        }


        Vec3d offset = Helper.getChainOffset(startPos, endPos);
        startPos = startPos.add(offset.getX(), 0, offset.getZ());
        endPos = endPos.add(-offset.getX(), 0, -offset.getZ());

        double distance = startPos.distanceTo(endPos);

        double x = MathHelper.lerp(v, startPos.getX(), endPos.getX());
        double y = startPos.getY() + Helper.drip2((v * distance), distance, endPos.getY() - startPos.getY());
        double z = MathHelper.lerp(v, startPos.getZ(), endPos.getZ());

        y += -ModEntityTypes.CHAIN_COLLISION.getHeight() + 2 / 16f;

        ChainCollisionEntity c = new ChainCollisionEntity(primary.world, x, y, z, this);
        if (primary.world.spawnEntity(c)) {
            return c;
        } else {
            ConnectibleChains.LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    /**
     * Finds all players that are in {@code world} and tracking either the primary or secondary.
     *
     * @param world the world to search in
     * @return A set of all players that track the primary or secondary.
     */
    private Set<ServerPlayerEntity> getTrackingPlayers(World world) {
        assert world instanceof ServerWorld;
        Set<ServerPlayerEntity> trackingPlayers = new HashSet<>(
                PlayerLookup.around((ServerWorld) world, primary.getBlockPos(), ChainKnotEntity.VISIBLE_RANGE));
        trackingPlayers.addAll(
                PlayerLookup.around((ServerWorld) world, secondary.getBlockPos(), ChainKnotEntity.VISIBLE_RANGE));
        return trackingPlayers;
    }

    public boolean isDead() {
        return !alive;
    }

    /**
     * Returns the squared distance between the primary and secondary.
     */
    public double getSquaredDistance() {
        return this.primary.squaredDistanceTo(secondary);
    }

    /**
     * Two links are considered equal when the involved entities are the same, regardless of their designation
     * and the links have the same living status.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainLink link = (ChainLink) o;

        boolean partnersEqual = primary.equals(link.primary) && secondary.equals(link.secondary) ||
                primary.equals(link.secondary) && secondary.equals(link.primary);
        return alive == link.alive && partnersEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, secondary, alive);
    }

    /**
     * If due to some error, or unforeseeable causes such as commands
     * the link still exists but needs to be destroyed.
     *
     * @return true when {@link #destroy(boolean)} needs to be called
     */
    public boolean needsBeDestroyed() {
        return primary.isRemoved() || secondary.isRemoved();
    }

    /**
     * Destroys the link including all collision entities and drops an item in its center when the conditions allow it. <br/>
     * This method is idempotent.
     *
     * @param mayDrop if an item may drop.
     */
    public void destroy(boolean mayDrop) {
        if (!alive) return;

        boolean drop = mayDrop;
        World world = primary.world;
        this.alive = false;

        if (world.isClient) return;

        if (secondary instanceof PlayerEntity player && player.isCreative()) drop = false;
        // I think DO_TILE_DROPS makes more sense than DO_ENTITY_DROPS in this case
        if (!world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) drop = false;

        if (drop) {
            ItemStack stack = new ItemStack(sourceItem);
            if (secondary instanceof PlayerEntity player) {
                player.giveItemStack(stack);
            } else {
                Vec3d middle = Helper.middleOf(primary.getPos(), secondary.getPos());
                ItemEntity itemEntity = new ItemEntity(world, middle.x, middle.y, middle.z, stack);
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }

        destroyCollision();
        if (!primary.isRemoved() && !secondary.isRemoved())
            sendDetachChainPacket(world);
    }

    /**
     * Removes the collision entities associated with this link.
     */
    private void destroyCollision() {
        for (Integer entityId : collisionStorage) {
            Entity e = primary.world.getEntityById(entityId);
            if (e instanceof ChainCollisionEntity) {
                e.remove(Entity.RemovalReason.DISCARDED);
            } else {
                ConnectibleChains.LOGGER.warn("Collision storage contained reference to {} (#{}) which is not a collision entity.", e, entityId);
            }
        }
        collisionStorage.clear();
    }

    /**
     * Send a package to all the clients around this entity that notifies them of this link's destruction.
     */
    private void sendDetachChainPacket(World world) {
        assert world instanceof ServerWorld;

        Set<ServerPlayerEntity> trackingPlayers = getTrackingPlayers(world);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        // Write both ids so that the client can identify the link
        buf.writeVarInt(primary.getId());
        buf.writeVarInt(secondary.getId());

        for (ServerPlayerEntity player : trackingPlayers) {
            ServerPlayNetworking.send(player, NetworkingPackets.S2C_CHAIN_DETACH_PACKET_ID, buf);
        }
    }
}
