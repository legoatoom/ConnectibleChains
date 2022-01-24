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

import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.ChainType;
import com.github.legoatoom.connectiblechains.util.PacketCreator;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.function.Function;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for connections between chains.
 *
 * @author legoatoom
 */
public class ChainCollisionEntity extends Entity {

//    /**
//     * The chainKnot entity id that has a connection to another chainKnot with id {@link #endOwnerId}.
//     */
//    private int startOwnerId;
//    /**
//     * The chainKnot entity id that has a connection from another chainKnot with id {@link #startOwnerId}.
//     */
//    private int endOwnerId;

    @Environment(EnvType.SERVER)
    private ChainLink link;

    /**
     * On the client only the chainType information is present, for pick block mostly
     */
    @Environment(EnvType.CLIENT)
    private ChainType chainType;

    @SuppressWarnings("WeakerAccess")
    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world) {
        super(entityType, world);

    }

//    @SuppressWarnings("WeakerAccess")
//    public ChainCollisionEntity(World world, double x, double y, double z, int startOwnerId, int endOwnerId) {
//        this(ModEntityTypes.CHAIN_COLLISION, world);
//        this.startOwnerId = startOwnerId;
//        this.endOwnerId = endOwnerId;
//        this.setPosition(x, y, z);
//    }

    public ChainCollisionEntity(World world, double x, double y, double z, ChainLink link) {
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.link = link;
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDataTracker() {
        // Required by Entity
    }

    /**
     * When this entity is attacked by a player with a item that has Tag: {@link FabricToolTags#SHEARS},
     * it calls the {@link ChainKnotEntity#damageLink(boolean, ChainKnotEntity)} method
     * to destroy the link between the {@link #startOwnerId} and {@link #endOwnerId}
     *
     * @return true when damage was effective
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if(this.world.isClient) {
//            return !(source.getSource() instanceof PersistentProjectileEntity);
            return false;
        }

        if(source.isExplosive()) {
            link.destroy(true);
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
            // That would cause some serious sound spam and we wanna avoid that.
            playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        }
        return false;

//        Entity startOwner = this.world.getEntityById(startOwnerId);
//        Entity endOwner = this.world.getEntityById(endOwnerId);
//        Entity sourceEntity = source.getAttacker();
//        if (source.getSource() instanceof PersistentProjectileEntity) {
//            return false;
//        } else if (sourceEntity instanceof PlayerEntity player) {
 //               && startOwner instanceof ChainKnotEntity && endOwner instanceof ChainKnotEntity*/) {
//            if (!player.getMainHandStack().isEmpty() && FabricToolTags.SHEARS.contains(player.getMainHandStack().getItem())) {
//            if(tryBreakWith(player.getMainHandStack().getItem(), !player.isCreative())) {
//                return true;
//            } else {
//                playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
//                return false;
//            }
//                    ((ChainKnotEntity) startOwner).damageLink(isCreative, (ChainKnotEntity) endOwner);
//                ChainLink link = null;
//                for (ChainLink chainLink : ((ChainKnotEntity) startOwner).getLinks()) {
//                    if(chainLink.primary == startOwner && chainLink.secondary == endOwner) {
//                        link = chainLink;
//                        break;
//                    }
//                    if(chainLink.secondary == startOwner && chainLink.primary == endOwner) {
//                        link = chainLink;
//                        break;
//                    }
//                }
//                if(link != null) {
//                    link.destroy(!player.isCreative());
//                }
//            }
//        }
//        playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
//        return true;
    }

    private boolean tryBreakWith(Item item, boolean mayDrop) {
        if (FabricToolTags.SHEARS.contains(item)) {
            if(!world.isClient) link.destroy(mayDrop);
            return true;
        }
        return false;
    }

    /**
     * If this entity can even be collided with.
     * Different from {@link #isCollidable()} ()} as this tells if something can collide with this.
     *
     * @return true
     */
    @Override
    public boolean collides() {
        return !isRemoved();
    }

    /**
     * We don't want to be able to push the collision box of the chain.
     * @return false
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * We only allow the collision box to be rendered if a player is holding a item that has tag {@link FabricToolTags#SHEARS}.
     * This might be helpful when using F3+B to see the boxes of the chain.
     *
     * @return boolean - should the collision box be rendered.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.isHolding(item -> item.isIn(FabricToolTags.SHEARS))) {
            return super.shouldRender(distance);
        } else {
            return false;
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        // Required by Entity, but does nothing.
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        // Required by Entity, but does nothing.
    }

    /**
     * Makes sure that nothing can walk through it.
     */
    @Override
    public boolean isCollidable() {
        return true;
    }

    /**
     * What happens when this is attacked?
     * This method is called by {@link PlayerEntity#attack(Entity)} to allow an entity to choose what happens when
     * it is attacked. We don't want to play sounds when we attack it without shears, so that is why we override this.
     */
    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            return this.damage(DamageSource.player(playerEntity), 0.0F);
        } else {
            playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
            return false;
        }
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
        boolean didBreak = tryBreakWith(player.getStackInHand(hand).getItem(), !player.isCreative());
        if(didBreak) return ActionResult.CONSUME;
        return ActionResult.PASS;
    }

    /**
     * When this entity is created we need to send a packet to the client.
     * This method sends a packet that contains the entityID of both the start and
     * end chainKnot of this entity.
     *
     */
    @Override
    public Packet<?> createSpawnPacket() {
        //Write our id and the id of the one we connect to.
        Function<PacketByteBuf, PacketByteBuf> extraData = packetByteBuf -> {
//            packetByteBuf.writeVarInt(startOwnerId);
//            packetByteBuf.writeVarInt(endOwnerId);
//            packetByteBuf.writeVarInt(link.primary.getId());
//            packetByteBuf.writeVarInt(link.secondary.getId());
            packetByteBuf.writeVarInt(Registry.ITEM.getRawId(link.chainType.getItem()));
            return packetByteBuf;
        };
        return PacketCreator.createSpawn(this, NetworkingPackets.S2C_SPAWN_CHAIN_COLLISION_PACKET, extraData);
    }

//    public void setStartOwnerId(int startOwnerId) {
//        this.startOwnerId = startOwnerId;
//    }
//
//    public int getStartOwnerId() {
//        return startOwnerId;
//    }
//
//    public void setEndOwnerId(int endOwnerId) {
//        this.endOwnerId = endOwnerId;
//    }
//
//    public int getEndOwnerId() {
//        return endOwnerId;
//    }

    public void setLink(ChainLink link) {
        this.link = link;
    }

    public ChainLink getLink() {
        return link;
    }

    public void setChainType(ChainType chainType) {
        this.chainType = chainType;
    }

    public ChainType getChainType() {
        return chainType;
    }

    @Override
    public void tick() {
        if(world.isClient) return;
        // Condition can be met when the knots were removed with commands
        // but the collider still exists
        if(link.needsBeDestroyed()) link.destroy(true);

        // Collider removes itself when the link is dead
        if(!this.isRemoved() && link.isDead()) {
            remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
