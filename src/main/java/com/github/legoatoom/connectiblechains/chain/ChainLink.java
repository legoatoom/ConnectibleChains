package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.enitity.ChainCollisionEntity;
import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.enitity.ModEntityTypes;
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
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChainLink {
    /**
     * The x/z distance between {@link ChainCollisionEntity ChainCollisionEntities}.
     * A value of 1 means they are "shoulder to shoulder"
     */
    private static final float COLLIDER_SPACING = 1.5f;

    public final ChainKnotEntity primary;
    public final Entity secondary;
    public final ChainType chainType;
    private final IntList collisionStorage = new IntArrayList(16);
    private boolean alive = true;
    public boolean removeSilently = false;

    public ChainLink(ChainKnotEntity primary, Entity secondary, ChainType chainType) {
        if(primary == null)
            throw new IllegalStateException("Tried to create a link from null");
        if(primary.equals(secondary))
            throw new IllegalStateException("Tried to create a link between a knot and itself");
        if(secondary == null)
            throw new IllegalStateException("Tried to create a link between a knot and null");
        this.primary = primary;
        this.secondary = secondary;
        this.chainType = chainType;
    }

    public boolean isDead() {
        return !alive;
    }

    /**
     * If due to some error, or unforeseeable causes such as commands
     * the link still exists but needs to be destroyed.
     * @return true when {@link #destroy(boolean)} needs to be called
     */
    public boolean needsBeDestroyed() {
        return primary.isRemoved() || secondary.isRemoved();
    }

    public double getSquaredDistance() {
        return this.primary.squaredDistanceTo(secondary);
    }

    /**
     * Create a chain link between primary and secondary and adds it to thier lists
     * @param primary A chain knot
     * @param secondary A different chain knot or player
     * @param chainType The link chainType
     * @return A new chain link or null if it already exists
     */
    @Nullable
    public static ChainLink create(ChainKnotEntity primary, Entity secondary, ChainType chainType) {
        ChainLink link = new ChainLink(primary, secondary, chainType);
        // Prevent multiple links between same targets
        if(primary.getLinks().contains(link)) return null;

        primary.addLink(link);
        if(secondary instanceof ChainKnotEntity secondaryKnot) {
            secondaryKnot.addLink(link);
            link.createCollision();
        }
        if(!primary.world.isClient) {
            link.sendAttachChainPacket(primary.world);
        }
        return link;
    }

    public void destroy(boolean mayDrop) {
        if(!alive) return;

        boolean drop = mayDrop;
        World world = primary.world;
        this.alive = false;

        if(world.isClient) return;

        if(secondary instanceof PlayerEntity player && player.isCreative()) drop = false;
        // I think DO_TILE_DROPS makes more sense than DO_ENTITY_DROPS in this case
        if(!world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) drop = false;

        if(drop) {
            ItemStack stack = new ItemStack(chainType.getItem());
            if(secondary instanceof PlayerEntity player) {
                player.giveItemStack(stack);
            } else {
                Vec3d middle = Helper.middleOf(primary.getPos(), secondary.getPos());
                ItemEntity itemEntity = new ItemEntity(world, middle.x, middle.y, middle.z, stack);
                itemEntity.setToDefaultPickupDelay();
                world.spawnEntity(itemEntity);
            }
        }

        destroyCollision();
        if(!primary.isRemoved() && !secondary.isRemoved())
            sendDetachChainPacket(world);
    }

    private Set<ServerPlayerEntity> getTrackingPlayers(World world) {
        assert world instanceof ServerWorld;
        Set<ServerPlayerEntity> trackingPlayers = new HashSet<>(
                PlayerLookup.around((ServerWorld) world, primary.getBlockPos(), ChainKnotEntity.VISIBLE_RANGE));
        trackingPlayers.addAll(
                PlayerLookup.around((ServerWorld) world, secondary.getBlockPos(), ChainKnotEntity.VISIBLE_RANGE));
        return trackingPlayers;
    }

    /**
     * Send a package to all the clients around this entity that specifies it want's to detach.
     */
    private void sendAttachChainPacket(World world) {
        assert world instanceof ServerWorld;

        Set<ServerPlayerEntity> trackingPlayers = getTrackingPlayers(world);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        // Write our id and the id of the one we connect to.
        buf.writeVarInt(primary.getId());
        buf.writeVarInt(secondary.getId());
        buf.writeVarInt(Registry.ITEM.getRawId(chainType.getItem()));

        for (ServerPlayerEntity player : trackingPlayers) {
            ServerPlayNetworking.send(player, NetworkingPackets.S2C_CHAIN_ATTACH_PACKET_ID, buf);
        }
    }

    /**
     * Send a package to all the clients around this entity that specifies it want's to detach.
     */
    private void sendDetachChainPacket(World world) {
        assert world instanceof ServerWorld;

        Set<ServerPlayerEntity> trackingPlayers = getTrackingPlayers(world);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());

        // Write our id and the id of the one we connect to.
        buf.writeVarInt(primary.getId());
        buf.writeVarInt(secondary.getId());

        for (ServerPlayerEntity player : trackingPlayers) {
            ServerPlayNetworking.send(player, NetworkingPackets.S2C_CHAIN_DETACH_PACKET_ID, buf);
        }
    }

    /**
     * Create a collision between this and an entity.
     * It spawns multiple {@link ChainCollisionEntity ChainCollisionEntities} that are equal distance from each other.
     * Position is the same no matter what if the connection is from A -> B or A <- B.
     */
    private void createCollision() {
        if (!collisionStorage.isEmpty()) return;
        if(primary.world.isClient) return;

        double distance = primary.distanceTo(secondary);
        double step = COLLIDER_SPACING*Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.getWidth(), 2)*2) / distance;
        double v = step;
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.getWidth() / distance;

        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, primary, secondary, v);
            if(collider1 != null) collisionStorage.add(collider1.getId());
            Entity collider2 = spawnCollision(true, primary, secondary, v);
            if(collider2 != null) collisionStorage.add(collider2.getId());

            v += step;
        }

        Entity centerCollider = spawnCollision(false,primary, secondary, 0.5);
        if(centerCollider != null) collisionStorage.add(centerCollider.getId());
    }

    /**
     * Remove a collision between this and an entity.
     */
    private void destroyCollision() {
        for (Integer entityId : collisionStorage) {
            Entity e = primary.world.getEntityById(entityId);
            if (e instanceof ChainCollisionEntity) {
                e.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    /**
     * Spawns a collider at v percent between entity1 and entity2
     * @param reverse Reverse start and end
     * @param start the entity at v=0
     * @param end the entity at v=1
     * @param v percent of the distance
     * @return {@link ChainCollisionEntity} or null
     */
    @Nullable
    private Entity spawnCollision(boolean reverse, Entity start, Entity end, double v) {
        assert primary.world instanceof ServerWorld;
        Vec3d startPos = start.getPos().add(start.getLeashOffset());
        Vec3d endPos = end.getPos().add(end.getLeashOffset());

        Vec3d tmp = endPos;
        if(reverse) {
            endPos = startPos;
            startPos = tmp;
        }

        Vec3f offset = Helper.getChainOffset(startPos, endPos);
        startPos = startPos.add(offset.getX(), 0, offset.getZ());
        endPos = endPos.add(-offset.getX(), 0, -offset.getZ());

        double distance = startPos.distanceTo(endPos);

        double x = MathHelper.lerp(v, startPos.getX(), endPos.getX());
        double y = startPos.getY() + Helper.drip2((v * distance), distance, endPos.getY() - startPos.getY());
        double z = MathHelper.lerp(v, startPos.getZ(), endPos.getZ());

        y += -ModEntityTypes.CHAIN_COLLISION.getHeight() + 1/16f;

        ChainCollisionEntity c = new ChainCollisionEntity(primary.world, x, y, z, this);
        if (primary.world.spawnEntity(c)) {
            return c;
        } else {
            ConnectibleChains.LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainLink link = (ChainLink) o;
        return alive == link.alive && (
                (primary.equals(link.primary) && secondary.equals(link.secondary)) ||
                (primary.equals(link.secondary) && secondary.equals(link.primary)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, secondary, alive);
    }
}
