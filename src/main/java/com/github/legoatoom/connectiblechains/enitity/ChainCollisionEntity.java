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
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;
import com.github.legoatoom.connectiblechains.tag.CommonTags;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import com.github.legoatoom.connectiblechains.util.PacketCreator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for links.
 *
 * @author legoatoom, Qendolin
 */
public class ChainCollisionEntity extends Entity implements ChainLinkEntity {

    /**
     * The link that this collider is a part of.
     */
    @Nullable
    private ChainLink link;

    /**
     * On the client only the chainType information is present, for the pick item action mostly
     */
    @Environment(EnvType.CLIENT)
    private ChainType chainType;

    public ChainCollisionEntity(World world, double x, double y, double z, @NotNull ChainLink link) {
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.link = link;
        this.setPosition(x, y, z);
    }

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world) {
        super(entityType, world);
    }

    @SuppressWarnings("unused")
    public @Nullable ChainLink getLink() {
        return link;
    }

    @Environment(EnvType.CLIENT)
    public ChainType getChainType() {
        return chainType;
    }

    @Environment(EnvType.CLIENT)
    public void setChainType(ChainType chainType) {
        this.chainType = chainType;
    }

    @Override
    protected void initDataTracker() {
    }

    /**
     * If this entity can even be collided with.
     * Different from {@link #isCollidable()} as this tells if something can collide with this.
     *
     * @return true when not removed.
     */
    @Override
    public boolean collides() {
        return !isRemoved();
    }

    /**
     * We don't want to be able to push the collision box of the chain.
     *
     * @return false
     */
    @Override
    public boolean isPushable() {
        return false;
    }

    /**
     * We only allow the collision box to be rendered if a player is holding a shears type item.
     * This might be helpful when using F3+B to see the boxes of the chain.
     *
     * @param distance the camera distance from the collider.
     * @return true when it should be rendered
     */
    @Environment(EnvType.CLIENT)
    @Override
    public boolean shouldRender(double distance) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.isHolding(CommonTags::isShear)) {
            return super.shouldRender(distance);
        } else {
            return false;
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
    }

    /**
     * Makes sure that nothing can walk through it.
     *
     * @return true
     */
    @Override
    public boolean isCollidable() {
        return true;
    }

    /**
     * @see ChainKnotEntity#handleAttack(Entity)
     */
    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            this.damage(DamageSource.player(playerEntity), 0.0F);
        } else {
            playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        }
        return true;
    }

    /**
     * @see ChainKnotEntity#damage(DamageSource, float)
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

    @Override
    public void destroyLinks(boolean mayDrop) {
        if (link != null) link.destroy(mayDrop);
    }

    /**
     * Interaction (attack or use) of a player and this entity.
     * Tries to destroy the link with the item in the players hand.
     *
     * @param player The player that interacted.
     * @param hand   The hand that interacted.
     * @return {@link ActionResult#SUCCESS} when the interaction was successful.
     */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (ChainLinkEntity.canDestroyWith(player.getStackInHand(hand))) {
            destroyLinks(!player.isCreative());
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    /**
     * The client only needs to know type info for the pick item action.
     * Links are handled server-side.
     */
    @Override
    public Packet<?> createSpawnPacket() {
        Function<PacketByteBuf, PacketByteBuf> extraData = packetByteBuf -> {
            ChainType chainType = link == null ? ChainTypesRegistry.DEFAULT_CHAIN_TYPE : link.chainType;
            packetByteBuf.writeVarInt(ChainTypesRegistry.REGISTRY.getRawId(chainType));
            return packetByteBuf;
        };
        return PacketCreator.createSpawn(this, NetworkingPackets.S2C_SPAWN_CHAIN_COLLISION_PACKET, extraData);
    }

    /**
     * Destroys broken links and removes itself when there is no alive link.
     */
    @Override
    public void tick() {
        if (world.isClient) return;
        // Condition can be met when the knots were removed with commands
        // but the collider still exists
        if (link != null && link.needsBeDestroyed()) link.destroy(true);

        // Collider removes itself when the link is dead
        if (link == null || link.isDead()) {
            remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
