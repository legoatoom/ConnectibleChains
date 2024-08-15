/*
 * Copyright (C) 2024 legoatoom.
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

package com.github.legoatoom.connectiblechains.entity;

import com.github.legoatoom.connectiblechains.chain.ChainLink;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    private Item linkSourceItem;


    public ChainCollisionEntity(World world, double x, double y, double z, @NotNull ChainLink link) {
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.link = link;
        this.setPosition(x, y, z);
        this.linkSourceItem = link.sourceItem;
    }

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world) {
        super(entityType, world);
    }

    @SuppressWarnings("unused")
    public @Nullable ChainLink getLink() {
        // Only available in the server. In the client, it is null.
        return link;
    }

    public @NotNull Item getLinkSourceItem() {
        // Always available.
        return linkSourceItem;
    }

    @Override
    public boolean canHit() {
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
        if (player != null && player.isHolding(itemStack -> itemStack.isIn(ConventionalItemTags.SHEAR_TOOLS))) {
            return super.shouldRender(distance);
        } else {
            return false;
        }
    }

    @Override
    public boolean isFireImmune() {
        return super.isFireImmune();
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
            this.damage(this.getDamageSources().playerAttack(playerEntity), 0.0F);
        } else {
            playSound(getHitSound(), 0.5F, 1.0F);
        }
        return true;
    }


    /**
     * @see ChainKnotEntity#damage(DamageSource, float)
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        ActionResult result = ChainLinkEntity.onDamageFrom(this, source, getHitSound());

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

    private SoundEvent getHitSound() {
        if (link != null) {
            return ChainLink.getSoundGroup(link.sourceItem).getHitSound();
        } else {
            return ChainLink.getSoundGroup(null).getHitSound();
        }
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

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        int id = Registries.ITEM.getRawId(linkSourceItem);
        return new EntitySpawnS2CPacket(this, entityTrackerEntry, id);
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        int rawChainItemSourceId = packet.getEntityData();
        linkSourceItem = Registries.ITEM.get(rawChainItemSourceId);
    }

    /**
     * Destroys broken links and removes itself when there is no alive link.
     */
    @Override
    public void tick() {
        if (getWorld().isClient()) return;
        // Condition can be met when the knots were removed with commands
        // but the collider still exists
        if (link != null && link.needsBeDestroyed()) link.destroy(true);

        // Collider removes itself when the link is dead
        if (link == null || link.isDead()) {
            remove(Entity.RemovalReason.DISCARDED);
        }
    }
}
