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
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The ChainKnotEntity is the main entity of this mod.
 * It has connections between others of its kind, and is a combination of {@link net.minecraft.entity.mob.MobEntity}
 * and {@link net.minecraft.entity.decoration.LeashKnotEntity}.
 *
 * @author legoatoom
 */
public class ChainKnotEntity extends AbstractDecorationEntity {
    
    /**
     * The distance when it is visible.
     */
    public static final double VISIBLE_RANGE = 2048.0D;

    /**
     * Ticks where the knot can live without any links.
     * This is important for 2 reasons: When the world loads a 'secondary' knot might load before it's 'primary'
     * In which case the knot would remove itself as it has no links and when the 'primary' loads it fails to create
     * a connection to this as this is already removed. The second use is for /summon for basically the same reasons.
     */
    private static final byte GRACE_PERIOD = 100;

    /**
     * The Tag that stores all the links
     */
    private NbtList chainTags;

    /**
     * A timer integer for destroying this entity if it isn't connected anything.
     */
    private int obstructionCheckCounter;

    /**
     * All links that involve this knot (secondary and primary)
     */
    private final ObjectList<ChainLink> links = new ObjectArrayList<>();

    /**
     * The chain chainType, for rendering
     */
    private ChainType chainType = ConnectibleChains.TYPES.getDefaultType();

    private byte graceTicks = GRACE_PERIOD;

    public ChainKnotEntity(EntityType<? extends ChainKnotEntity> entityType, World world) {
        super(entityType, world);
    }

    public ChainKnotEntity(World world, BlockPos pos, ChainType chainType) {
        super(ModEntityTypes.CHAIN_KNOT, world, pos);
        this.setPosition((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D);
        this.chainType = chainType;
    }

    public void setChainType(ChainType chainType) {
        this.chainType = chainType;
    }

    public ChainType getChainType() {
        return chainType;
    }

    /**
     * This method tries to check if around the target there are other {@link ChainKnotEntity ChainKnotEntities} that
     * have a connection to this player, if so we remove it and create a new one to destination
     *
     * @param playerEntity the player wo tries to make a connection.
     * @param world The current world.
     * @param pos the position where we want to make a chainKnot.
     * @param destination the destination knot where the links will be moved to
     * @return boolean, if it has made a connection.
     */
    public static boolean tryAttachHeldChainsToBlock(PlayerEntity playerEntity, World world, BlockPos pos, ChainKnotEntity destination) {
        boolean hasMadeConnection = false;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        List<ChainKnotEntity> otherKnots = world.getNonSpectatingEntities(ChainKnotEntity.class,
                new Box(x - getMaxRange(), y - getMaxRange(), z - getMaxRange(),
                        x + getMaxRange(), y + getMaxRange(), z + getMaxRange()));

        for (ChainKnotEntity source : otherKnots) {
            // Prevent connections with self
            if(destination.equals(source)) continue;

            // Prevent CME because ChainLink.create adds a link
            int linksCount = source.getLinks().size();
            for (int i = 0; i < linksCount; i++) {
                ChainLink link = source.getLinks().get(i);
                if(link.secondary != playerEntity) continue;
                // We found a knot that is connected to the player.

                // Now move that link to this knot
                ChainLink newLink = ChainLink.create(source, destination, link.chainType);

                // Check if the link does not already exist
                if(newLink != null) {
                    link.destroy(false);
                    link.removeSilently = true;
                    hasMadeConnection = true;
                }
            }
        }
        return hasMadeConnection;
    }

    /**
     * The max range of the chain.
     */
    public static double getMaxRange() {
        return ConnectibleChains.runtimeConfig.getMaxChainRange();
    }

    /**
     * This entity does not want to set a facing.
     */
    public void setFacing(Direction facing) {
    }

    /**
     * Update the position of this chain to the position of the block this is attached too.
     */
    protected void updateAttachmentPosition() {
        this.setPos((double) this.attachmentPos.getX() + 0.5D, (double) this.attachmentPos.getY() + 0.5D, (double) this.attachmentPos.getZ() + 0.5D);
        double w = this.getType().getWidth() / 2.0;
        double h = this.getType().getHeight();
        this.setBoundingBox(new Box(this.getX() - w, this.getY(), this.getZ() - w, this.getX() + w, this.getY() + h, this.getZ() + w));
    }

    /**
     * This happens every tick.
     * It deletes the chainEntity if it is in the void.
     * It updates the chains, see {@link #updateChains()}
     * It checks if it is still connected to a block every 100 ticks.
     */
    @Override
    public void tick() {
        if (this.world.isClient) {
            // All logic in handled on the server. The client only knows enough to render the entity.
            return;
        }
        if (this.getY() < -64.0D) {
            this.tickInVoid();
        }
        this.updateChains();

        if (this.obstructionCheckCounter++ == 100) {
            this.obstructionCheckCounter = 0;
            if (!isRemoved() && !this.canStayAttached()) {
                for (ChainLink link : links) {
                    link.destroy(true);
                }
            }
        }

        removeDeadLinks();
        graceTicks--;
    }

    private void removeDeadLinks() {
        boolean playBreakSound = false;
        for (ChainLink link : links) {
            if(link.needsBeDestroyed()) link.destroy(true);
            if(link.isDead() && !link.removeSilently) playBreakSound = true;
        }
        if(playBreakSound) onBreak(null);

        links.removeIf(ChainLink::isDead);
        if(links.isEmpty() && (chainTags == null || chainTags.isEmpty()) && graceTicks <= 0) {
            this.remove(RemovalReason.DISCARDED);
            // No break sound
        }
    }

    /**
     * Simple checker to see if the block is connected to a fence or a wall.
     * @return boolean - if it can stay attached.
     */
    public boolean canStayAttached() {
        Block block = this.world.getBlockState(this.attachmentPos).getBlock();
        return canConnectTo(block);
    }

    /**
     * Is this block acceptable to connect too?
     * @param block the block in question.
     * @return boolean if is allowed or not.
     */
    public static boolean canConnectTo(Block block){
        return BlockTags.WALLS.contains(block) || BlockTags.FENCES.contains(block);
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        if (attacker instanceof PlayerEntity playerEntity) {
            return this.damage(DamageSource.player(playerEntity), 0.0F);
        } else {
            return false;
        }
    }

    /**
     * When this entity is being attacked, we remove all connections and then remove this entity.
     * @return if it is successfully attacked.
     */
    @Override
    public boolean damage(DamageSource source, float amount) {

        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if(this.world.isClient) {
            return false;
        }

        if(source.isExplosive()) {
            for (ChainLink link : links) {
                link.destroy(true);
            }
            return true;
        }
        if(source.getSource() instanceof PlayerEntity player) {
            if(tryBreakWith(player.getMainHandStack().getItem(), !player.isCreative())) {
                return true;
            }
        }

        if(!source.isProjectile()) {
            // Projectiles such as arrows (actually probably just arrows) can get "stuck"
            // on entities they cannot damage, such as players while blocking with shields or these chains.
            // That would cause some serious sound spam, and we want to avoid that.
            playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        }
        return false;
    }

    private boolean tryBreakWith(Item item, boolean mayDrop) {
        if (FabricToolTags.SHEARS.contains(item)) {
            if(!world.isClient) {
                for (ChainLink link : links) {
                    link.destroy(mayDrop);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Method to write all connections in a {@link NbtCompound} when we save the game.
     *
     * @param tag the tag to write info in.
     */
    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        tag.putString("ChainType", chainType.item().toString());
        NbtList linksTag = new NbtList();
        for (ChainLink link : this.links) {
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
        if (!linksTag.isEmpty()) {
            tag.put("Chains", linksTag);
        }
    }

    /**
     * Read all the info into the {@link #chainTags}
     * We do not make connections here because not all entities might be loaded yet.
     *
     * @param tag the tag to read from.
     */
    public void readCustomDataFromNbt(NbtCompound tag) {
        if (tag.contains("Chains")) {
            this.chainTags = tag.getList("Chains", 10);
        }
        this.chainType = ConnectibleChains.TYPES.getOrDefault(Identifier.tryParse(tag.getString("ChainType")));
    }

    public int getWidthPixels() {
        return 9;
    }

    public int getHeightPixels() {
        return 9;
    }

    public void onBreak(@Nullable Entity entity) {
        this.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0F, 1.0F);
    }

    public void onPlace() {
        this.playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
    }

    public void setPosition(double x, double y, double z) {
        super.setPosition((double) MathHelper.floor(x) + 0.5D, (double) MathHelper.floor(y) + 0.5D, (double) MathHelper.floor(z) + 0.5D);
    }

    public void setGraceTicks(byte graceTicks) {
        this.graceTicks = graceTicks;
    }

    public List<ChainLink> getLinks() {
        return links;
    }

    public void updateChainType(ChainType chainType) {
        this.chainType = chainType;

        if(!world.isClient) {
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
     * This method will call {@link #deserializeChainTag(NbtElement)} if the {@link #chainTags} has any tags.
     * It will also break all connections that are larger than the {@link #getMaxRange()}
     */
    private void updateChains() {
        if (chainTags != null) {
            chainTags.removeIf(this::deserializeChainTag);
        }

        double squaredMaxRange = getMaxRange() * getMaxRange();
        for(ChainLink link : links) {
            if(link.isDead()) continue;

            if(!this.isAlive()) {
                link.destroy(true);
            } else if(link.primary == this && link.getSquaredDistance() > squaredMaxRange) {
                // no need to check the distance on both ends
                link.destroy(true);
            }
        }
    }

    /**
     * This method tries to connect to an entity that is in the {@link #chainTags}.
     * If they do not exist yet, we skip them. If they do, make a connection and remove it from the tag.
     * <p>
     * If when the {@link #age} of this entity is bigger than 100, we remove the tag from the {@link #chainTags}
     * meaning that we cannot find the connection anymore, and we assume that it will not be loaded in the future.
     *
     * @param element the tag that contains a single connection.
     * @return true if the tag has been used
     * @see #updateChains()
     */
    private boolean deserializeChainTag(NbtElement element) {
        if (element == null || !(this.world instanceof ServerWorld)) {
            return false;
        }

        assert element instanceof NbtCompound;
        NbtCompound tag = (NbtCompound) element;

        ChainType chainType = ConnectibleChains.TYPES.getOrDefault(Identifier.tryParse(tag.getString("ChainType")));

        if (tag.contains("UUID")) {
            UUID uuid = tag.getUuid("UUID");
            Entity entity = ((ServerWorld) this.world).getEntity(uuid);
            if (entity != null) {
                ChainLink.create(this, entity, chainType);
                return true;
            }
        } else if (tag.contains("X")) {
            BlockPos blockPos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            ChainKnotEntity entity = ChainKnotEntity.get(this.world, blockPos);
            if (entity != null) {
                ChainLink.create(this, entity, chainType);
                return true;
            }
        }

        // At the start the server and client need to tell each other the info.
        // So we need to check if the object is old enough for these things to exist before we delete them.
        if (this.graceTicks <= 0) {
            this.dropItem(chainType.getItem());
            this.onBreak(null);
            return true;
        }

        return false;
    }

    public void addLink(ChainLink link) {
        this.links.add(link);
    }

    public static ChainKnotEntity get(World world, BlockPos pos) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();
        final List<ChainKnotEntity> list = world.getNonSpectatingEntities(ChainKnotEntity.class,
                new Box((double) posX - 1.0D, (double) posY - 1.0D, (double) posZ - 1.0D,
                        (double) posX + 1.0D, (double) posY + 1.0D, (double) posZ + 1.0D));
        Iterator<ChainKnotEntity> iterator = list.iterator();

        ChainKnotEntity result = null;
        while (iterator.hasNext()) {
            ChainKnotEntity current = iterator.next();
            if(current.getDecorationBlockPos().equals(pos)) {
                result = current;
                break;
            }
        }

        return result;
    }

    /**
     * Should we render this?
     * @param distance the distance from the chainKnot.
     * @return boolean, yes or no.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        return distance < VISIBLE_RANGE;
    }

    /**
     * Interaction of a player and this entity.
     * It will try to make new connections to the player or allow other chains that are connected to the player to
     * be made to this.
     *
     * @param player the player that interacted.
     * @param hand the hand of the player.
     * @return ActionResult
     */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (this.world.isClient) {
            Item handItem = player.getStackInHand(hand).getItem();
            if(ConnectibleChains.TYPES.has(handItem)) {
                return ActionResult.SUCCESS;
            }

            if(tryBreakWith(handItem, !player.isCreative())) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        // 1. Try to move existing link from player to this.
        boolean madeConnection = tryAttachHeldChainsToBlock(player, world, getDecorationBlockPos(), this);
        if(madeConnection) {
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
        if(broke) {
            return ActionResult.CONSUME;
        }

        // 3. Try to create a new connection
        Item handItem = player.getStackInHand(hand).getItem();
        if(ConnectibleChains.TYPES.has(handItem)) {
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
        if(tryBreakWith(handItem, !player.isCreative())) {
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return -0.0625F;
    }

    /**
     * Spawn entity package.
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

    public Vec3d getLeashOffset() {
        return new Vec3d(0, 5/16f, 0);
    }

    /**
     * Not sure what this does but {@link net.minecraft.entity.decoration.LeashKnotEntity#getLeashPos(float)} has it.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public Vec3d getLeashPos(float f) {
        return this.getLerpedPos(f).add(0.0D, 5/16f, 0.0D);
    }
}
