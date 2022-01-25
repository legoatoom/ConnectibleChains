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

package com.github.legoatoom.connectiblechains.enitity;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import com.github.legoatoom.connectiblechains.util.PacketCreator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has links to others of its kind, and is a combination of {@link net.minecraft.entity.mob.MobEntity}
 * and {@link net.minecraft.entity.decoration.LeashKnotEntity}.
 *
 * @author legoatoom, Qendolin
 */
public class ChainKnotEntity extends AbstractDecorationEntity implements ChainLinkEntity {

    /**
     * The distance when it is visible.
     */
    public static final double VISIBLE_RANGE = 2048.0D;
    /**
     * Ticks where the knot can live without any links.
     * This is important for 2 reasons: When the world loads, a 'secondary' knot might load before it's 'primary'
     * In which case the knot would remove itself as it has no links and when the 'primary' loads it fails to create
     * a link to this as this is already removed. The second use is for /summon for basically the same reasons.
     */
    private static final byte GRACE_PERIOD = 100;
    /**
     * All links that involve this knot (secondary and primary)
     */
    private final ObjectList<ChainLink> links = new ObjectArrayList<>();
    /**
     * Links where the 'secondary' might not exist yet. Will be cleared after the grace period.
     */
    @Environment(EnvType.SERVER)
    private final ObjectList<NbtElement> incompleteLinks = new ObjectArrayList<>();
    /**
     * Increments each tick, when it reached 100 it resets and checks {@link #canStayAttached()}.
     */
    private int obstructionCheckTimer = 0;
    /**
     * The chain type, used for rendering
     */
    private ChainType chainType = ConnectibleChains.TYPES.getDefaultType();
    /**
     * Remaining grace ticks, will be set to 0 when the last incomplete link is removed.
     */
    private byte graceTicks = GRACE_PERIOD;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos, ChainType chainType) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        setPosition((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.chainType = chainType;
    }

    /**
     * Set the {@link #attachmentPos}.
     *
     * @see #updateAttachmentPosition()
     */
    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D, (double) MathHelper.floor(z) + 0.5D);
    }

    public ChainType getChainType() {
        return chainType;
    }

    public void setChainType(ChainType chainType) {
        this.chainType = chainType;
    }

    public void setGraceTicks(byte graceTicks) {
        this.graceTicks = graceTicks;
    }

    public void setFacing(Direction facing) {
    }

    /**
     * Update the position of this chain to the position of the block this is attached to.
     * Also updates the bounding box.
     */
    protected void updateAttachmentPosition() {
        setPos(attachmentPos.getX() + 0.5D, attachmentPos.getY() + 0.5D, attachmentPos.getZ() + 0.5D);
        double w = getType().getWidth() / 2.0;
        double h = getType().getHeight();
        setBoundingBox(new Box(getX() - w, getY(), getZ() - w, getX() + w, getY() + h, getZ() + w));
    }

    /**
     * On the server it:
     * <ol>
     * <li>Checks if its in the void and deletes itself.</li>
     * <li>Tries to convert incomplete links</li>
     * <li>Updates the chains, see {@link #updateLinks()}</li>
     * <li>Removes any dead links, and, when outside the grace period, itself if none are left.</li>
     * </ol>
     */
    @Override
    public void tick() {
        if (world.isClient) {
            // All logic in handled on the server. The client only knows enough to render the entity.
            return;
        }
        attemptTickInVoid();

        boolean anyConverted = convertIncompleteLinks();
        updateLinks();
        removeDeadLinks();

        if (graceTicks < 0 || (anyConverted && incompleteLinks.isEmpty())) {
            graceTicks = 0;
        } else if (graceTicks > 0) {
            graceTicks--;
        }
    }

    /**
     * Will try to convert any {@link #incompleteLinks} using {@link #deserializeChainTag(NbtElement)}.
     *
     * @return true if any were converted
     */
    private boolean convertIncompleteLinks() {
        if (!incompleteLinks.isEmpty()) {
            return incompleteLinks.removeIf(this::deserializeChainTag);
        }
        return false;
    }

    /**
     * Will break all connections that are larger than the {@link #getMaxRange()},
     * when this knot is dead, or can't stay attached.
     */
    private void updateLinks() {
        double squaredMaxRange = getMaxRange() * getMaxRange();
        for (ChainLink link : links) {
            if (link.isDead()) continue;

            if (!isAlive()) {
                link.destroy(true);
            } else if (link.primary == this && link.getSquaredDistance() > squaredMaxRange) {
                // no need to check the distance on both ends
                link.destroy(true);
            }
        }

        if (obstructionCheckTimer++ == 100) {
            obstructionCheckTimer = 0;
            if (!canStayAttached()) {
                destroyLinks(true);
            }
        }
    }

    /**
     * Removes any dead links and plays a break sound if any were removed.
     * Removes itself when no {@link #links} or {@link #incompleteLinks} are left, and it's outside the grace period.
     */
    private void removeDeadLinks() {
        boolean playBreakSound = false;
        for (ChainLink link : links) {
            if (link.needsBeDestroyed()) link.destroy(true);
            if (link.isDead() && !link.removeSilently) playBreakSound = true;
        }
        if (playBreakSound) onBreak(null);

        links.removeIf(ChainLink::isDead);
        if (links.isEmpty() && incompleteLinks.isEmpty() && graceTicks <= 0) {
            remove(RemovalReason.DISCARDED);
            // No break sound
        }
    }

    /**
     * This method tries to connect to the secondary that is in the {@link #incompleteLinks}.
     * If they do not exist yet, we try again later. If they do, make a connection and remove it from the tag.
     * <br>
     * When the grace period is over, we remove the tag from the {@link #incompleteLinks} and drop an item
     * meaning that we cannot find the connection anymore, and we assume that it will not be loaded in the future.
     *
     * @param element the tag that contains a single connection.
     * @return true if the tag has been used
     * @see #updateLinks()
     */
    private boolean deserializeChainTag(NbtElement element) {
        if (element == null || world.isClient) {
            return true;
        }

        assert element instanceof NbtCompound;
        NbtCompound tag = (NbtCompound) element;

        ChainType chainType = ConnectibleChains.TYPES.getOrDefault(Identifier.tryParse(tag.getString("ChainType")));

        if (tag.contains("UUID")) {
            UUID uuid = tag.getUuid("UUID");
            Entity entity = ((ServerWorld) world).getEntity(uuid);
            if (entity != null) {
                ChainLink.create(this, entity, chainType);
                return true;
            }
        } else if (tag.contains("X")) {
            BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            ChainKnotEntity entity = ChainKnotEntity.get(world, blockPos);
            if (entity != null) {
                ChainLink.create(this, entity, chainType);
                return true;
            }
        }

        // At the start the server and client need to tell each other the info.
        // So we need to check if the object is old enough for these things to exist before we delete them.
        if (graceTicks <= 0) {
            dropItem(chainType.getItem());
            onBreak(null);
            return true;
        }

        return false;
    }

    /**
     * The maximum distance between two knots.
     */
    public static double getMaxRange() {
        return ConnectibleChains.runtimeConfig.getMaxChainRange();
    }

    /**
     * Simple checker to see if the block is connected to a fence or a wall.
     *
     * @return true if it can stay attached.
     */
    public boolean canStayAttached() {
        Block block = world.getBlockState(attachmentPos).getBlock();
        return canAttachTo(block);
    }

    @Override
    public void destroyLinks(boolean mayDrop) {
        for (ChainLink link : links) {
            link.destroy(mayDrop);
        }
    }

    @Override
    public void onBreak(@Nullable Entity entity) {
        playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0F, 1.0F);
    }

    /**
     * Searches for a knot at {@code pos} and returns it.
     *
     * @param world The world to search in.
     * @param pos   The position to search at.
     * @return {@link ChainKnotEntity} or null when none exists at {@code pos}.
     */
    @Nullable
    public static ChainKnotEntity get(World world, BlockPos pos) {
        List<ChainKnotEntity> results = world.getNonSpectatingEntities(ChainKnotEntity.class,
                Box.of(Vec3d.of(pos), 2, 2, 2));

        for (ChainKnotEntity current : results) {
            if (current.getDecorationBlockPos().equals(pos)) {
                return current;
            }
        }

        return null;
    }

    /**
     * Is this block acceptable to attach a knot?
     *
     * @param block the block in question.
     * @return true if is allowed.
     */
    public static boolean canAttachTo(Block block) {
        return BlockTags.WALLS.contains(block) || BlockTags.FENCES.contains(block);
    }

    /**
     * Calls {@link #damage(DamageSource, float)} when attacked by a player. Plays a hit sound otherwise. <br/>
     * It is used by {@link PlayerEntity#attack(Entity)} where a true return value indicates
     * that this entity handled the attack and no further actions should be made.
     *
     * @param attacker The source of the attack.
     * @return true
     */
    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            damage(DamageSource.player(playerEntity), 0.0F);
        } else {
            playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        }
        return true;
    }

    /**
     * @return true when damage was effective
     * @see ChainKnotEntity#onDamageFrom(Entity, DamageSource)
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        ActionResult result = ChainLinkEntity.onDamageFrom(this, source);

        if (result.isAccepted()) {
            destroyLinks(result == ActionResult.SUCCESS);
            return true;
        }
        return false;
    }

    /**
     * Stores the {@link #chainType chain type} and all primary links
     * and old, incomplete links inside {@code root}
     *
     * @param root the tag to write info in.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound root) {
        root.putString("ChainType", chainType.item().toString());
        NbtList linksTag = new NbtList();

        // Write complete links
        for (ChainLink link : links) {
            if (link.isDead()) continue;
            if (link.primary != this) continue;
            Entity secondary = link.secondary;
            NbtCompound compoundTag = new NbtCompound();
            compoundTag.putString("ChainType", link.chainType.item().toString());
            if (secondary instanceof PlayerEntity) {
                UUID uuid = secondary.getUuid();
                compoundTag.putUuid("UUID", uuid);
            } else if (secondary instanceof AbstractDecorationEntity) {
                BlockPos blockPos = ((AbstractDecorationEntity) secondary).getDecorationBlockPos();
                compoundTag.putInt("X", blockPos.getX());
                compoundTag.putInt("Y", blockPos.getY());
                compoundTag.putInt("Z", blockPos.getZ());
            }
            linksTag.add(compoundTag);
        }

        // Write old, incomplete links
        linksTag.addAll(incompleteLinks);

        if (!linksTag.isEmpty()) {
            root.put("Chains", linksTag);
        }
    }

    /**
     * Read all the data from {@link #writeCustomDataToNbt(NbtCompound)}
     * and stores the links in {@link #incompleteLinks}.
     *
     * @param root the tag to read from.
     */
    public void readCustomDataFromNbt(NbtCompound root) {
        if (root.contains("Chains")) {
            incompleteLinks.addAll(root.getList("Chains", 10));
        }
        chainType = ConnectibleChains.TYPES.getOrDefault(Identifier.tryParse(root.getString("ChainType")));
    }

    @Override
    public int getWidthPixels() {
        return 9;
    }

    @Override
    public int getHeightPixels() {
        return 9;
    }

    /**
     * Checks if the {@code distance} is within the {@link #VISIBLE_RANGE visible range}.
     *
     * @param distance the camera distance from the knot.
     * @return true when it should be rendered
     */
    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        return distance < VISIBLE_RANGE;
    }

    @Override
    public Vec3d getLeashOffset() {
        return new Vec3d(0, 5 / 16f, 0);
    }

    /**
     * The offset where a leash / chain will visually connect to.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public Vec3d getLeashPos(float f) {
        return getLerpedPos(f).add(0.0D, 5 / 16f, 0.0D);
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 5 / 16f;
    }

    /**
     * Interaction (attack or use) of a player and this entity.
     * On the server it will:
     * <ol>
     * <li>Try to move existing link from player to this.</li>
     * <li>Try to cancel chain links (when clicking a knot that already has a connection to {@code player}).</li>
     * <li>Try to create a new connection.</li>
     * <li>Try to destroy the knot with the item in the players hand.</li>
     * </ol>
     *
     * @param player The player that interacted.
     * @param hand   The hand that interacted.
     * @return {@link ActionResult#SUCCESS} or {@link ActionResult#CONSUME} when the interaction was successful.
     * @see #tryAttachHeldChains(PlayerEntity)
     */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (world.isClient) {
            Item handItem = player.getStackInHand(hand).getItem();
            if (ConnectibleChains.TYPES.has(handItem)) {
                return ActionResult.SUCCESS;
            }

            if (ChainLinkEntity.canDestroyWith(handItem)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        // 1. Try to move existing link from player to this.
        boolean madeConnection = tryAttachHeldChains(player);
        if (madeConnection) {
            onPlace();
            return ActionResult.CONSUME;
        }

        // 2. Try to cancel chain links (when clicking same knot twice)
        boolean broke = false;
        for (ChainLink link : links) {
            if (link.secondary == player) {
                broke = true;
                link.destroy(true);
            }
        }
        if (broke) {
            return ActionResult.CONSUME;
        }

        // 3. Try to create a new connection
        Item handItem = player.getStackInHand(hand).getItem();
        if (ConnectibleChains.TYPES.has(handItem)) {
            // Interacted with a valid chain item, create a new link
            onPlace();
            ChainType chainType = ConnectibleChains.TYPES.get(handItem);
            ChainLink.create(this, player, chainType);
            if (!player.isCreative()) {
                player.getStackInHand(hand).decrement(1);
            }
            // Allow changing the chainType of the knot
            updateChainType(chainType);

            return ActionResult.CONSUME;
        }

        // 4. Interacted with anything else, check for shears
        if (ChainLinkEntity.canDestroyWith(handItem)) {
            destroyLinks(!player.isCreative());
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    /**
     * Searches for other {@link ChainKnotEntity ChainKnotEntities} around {@code itself} that
     * have a link to {@code player}, if so we remove it and create a new one to {@code itself}.
     *
     * @param player the player wo tries to make a connection.
     * @return true if it has made a connection.
     */
    public boolean tryAttachHeldChains(PlayerEntity player) {
        boolean hasMadeConnection = false;
        Box searchBox = Box.of(Vec3d.of(getDecorationBlockPos()), getMaxRange() * 2, getMaxRange() * 2, getMaxRange() * 2);
        List<ChainKnotEntity> otherKnots = world.getNonSpectatingEntities(ChainKnotEntity.class, searchBox);

        for (ChainKnotEntity source : otherKnots) {
            // Prevent connections with self
            if (source == this) continue;

            // Prevent CME because ChainLink.create adds a link
            int linksCount = source.getLinks().size();
            for (int i = 0; i < linksCount; i++) {
                ChainLink link = source.getLinks().get(i);
                if (link.secondary != player) continue;
                // We found a knot that is connected to the player.

                // Now move that link to this knot
                ChainLink newLink = ChainLink.create(source, this, link.chainType);

                // Check if the link does not already exist
                if (newLink != null) {
                    link.destroy(false);
                    link.removeSilently = true;
                    hasMadeConnection = true;
                }
            }
        }
        return hasMadeConnection;
    }

    @Override
    public void onPlace() {
        playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
    }

    /**
     * Sets the chain type and sends a packet to the client.
     *
     * @param chainType The new chain type.
     */
    public void updateChainType(ChainType chainType) {
        this.chainType = chainType;

        if (!world.isClient) {
            Collection<ServerPlayerEntity> trackingPlayers = PlayerLookup.around((ServerWorld) world, getBlockPos(), ChainKnotEntity.VISIBLE_RANGE);
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeVarInt(getId());
            buf.writeVarInt(Registry.ITEM.getRawId(chainType.getItem()));

            for (ServerPlayerEntity player : trackingPlayers) {
                ServerPlayNetworking.send(player, NetworkingPackets.S2C_KNOT_CHANGE_TYPE_PACKET, buf);
            }
        }
    }

    /**
     * @apiNote Operating on the list has potential for bugs as it does not include incomplete links.
     * For example {@link ChainLink#create(ChainKnotEntity, Entity, ChainType)} checks if the link already exists
     * using this list. Same goes for {@link #tryAttachHeldChains(PlayerEntity)}
     * but at the end of the day it doesn't really matter.
     * When an incomplete link is not resolved within the first two ticks it is unlikely to ever complete.
     * And even if it completes it will be stopped either because the knot is dead or the duplicates check in {@code ChainLink}.
     * @return all complete links that are associated with this knot.
     */
    public List<ChainLink> getLinks() {
        return links;
    }

    /**
     * Writes all client side relevant information into a {@link NetworkingPackets#S2C_SPAWN_CHAIN_KNOT_PACKET} packet and sends it.
     *
     * @see PacketCreator
     */
    @Override
    public Packet<?> createSpawnPacket() {
        Function<PacketByteBuf, PacketByteBuf> extraData = packetByteBuf -> {
            packetByteBuf.writeVarInt(Registry.ITEM.getRawId(chainType.getItem()));
            return packetByteBuf;
        };
        return PacketCreator.createSpawn(this, NetworkingPackets.S2C_SPAWN_CHAIN_KNOT_PACKET, extraData);
    }

    public void addLink(ChainLink link) {
        links.add(link);
    }
}
