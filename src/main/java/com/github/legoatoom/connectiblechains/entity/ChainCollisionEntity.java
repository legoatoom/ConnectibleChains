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

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.util.Helper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.github.legoatoom.connectiblechains.entity.Chainable.getSourceBlockSoundGroup;

/**
 * ChainCollisionEntity is an Entity that is invisible but has a collision.
 * It is used to create a collision for links.
 *
 * @author legoatoom, Qendolin
 */
public class ChainCollisionEntity extends Entity implements ChainLinkEntity {
    /**
     * The x/z distance between collision entities.
     * A value of 1 means they are "shoulder to shoulder"
     */
    private static final float COLLIDER_SPACING = 1.5f;

    /**
     * The link that this collider is a part of.
     * Only available in the server.
     */
    @Nullable
    private Chainable.ChainData link;

    private Entity chainedEntity;

    @NotNull
    private Item linkSourceItem;


    public ChainCollisionEntity(World world, double x, double y, double z, Entity chainedEntity, @NotNull Chainable.ChainData link) {
        this(ModEntityTypes.CHAIN_COLLISION, world);
        this.link = link;
        this.setPosition(x, y, z);
        this.linkSourceItem = link.sourceItem;
        this.chainedEntity = chainedEntity;
    }

    public ChainCollisionEntity(EntityType<? extends ChainCollisionEntity> entityType, World world) {
        super(entityType, world);
    }

    public static <E extends Entity & Chainable> void createCollision(E chainedEntity, Chainable.ChainData chainData) {
        if (chainedEntity.getWorld().isClient()) return;

        ServerWorld serverWorld = (ServerWorld) chainedEntity.getWorld();
        chainData.collisionStorage.removeIf(id -> serverWorld.getEntityById(id) == null);

        if (!chainData.collisionStorage.isEmpty()) return;

        // SERVER-SIDE //
        Entity chainHolder = chainedEntity.getChainHolder(chainData);

        if (chainHolder == null) {
            return;
        }

        double distance = chainedEntity.distanceTo(chainHolder);
        // step = spacing * √(width^2 + width^2) / distance
        double step = COLLIDER_SPACING * Math.sqrt(Math.pow(ModEntityTypes.CHAIN_COLLISION.getWidth(), 2) * 2) / distance;
        double v = step;
        // reserve space for the center collider
        double centerHoldout = ModEntityTypes.CHAIN_COLLISION.getWidth() / distance;

        while (v < 0.5 - centerHoldout) {
            Entity collider1 = spawnCollision(false, chainedEntity, chainData, v);
            if (collider1 != null) chainData.collisionStorage.add(collider1.getId());
            Entity collider2 = spawnCollision(true, chainedEntity, chainData, v);
            if (collider2 != null) chainData.collisionStorage.add(collider2.getId());

            v += step;
        }

        Entity centerCollider = spawnCollision(false, chainedEntity, chainData, 0.5);
        if (centerCollider != null) chainData.collisionStorage.add(centerCollider.getId());
    }

    public static <E extends Entity & Chainable> ChainCollisionEntity spawnCollision(boolean reverse, E chainedEntity, Chainable.ChainData chainData, double distancePercentage) {
        if (!(chainedEntity.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        Entity chainHolder = chainedEntity.getChainHolder(chainData);
        assert chainHolder != null;

        Vec3d srcPos = chainedEntity.getChainPos(1);
        Vec3d dstPos;
        if (chainHolder instanceof ChainKnotEntity chainKnotEntity) {
            dstPos = chainKnotEntity.getChainPos(1);
        } else {
            dstPos = chainHolder.getLeashPos(1);
        }

        Vec3d tmp = dstPos;
        if (reverse) {
            dstPos = srcPos;
            srcPos = tmp;
        }

        double distance = srcPos.distanceTo(dstPos);

        double x = MathHelper.lerp(distancePercentage, srcPos.getX(), dstPos.getX());
        double y = srcPos.getY() + Helper.drip2((distancePercentage * distance), distance, dstPos.getY() - srcPos.getY());
        double z = MathHelper.lerp(distancePercentage, srcPos.getZ(), dstPos.getZ());

        y += -ModEntityTypes.CHAIN_COLLISION.getHeight() + 2 / 16f;

        ChainCollisionEntity c = new ChainCollisionEntity(serverWorld, x, y, z, chainedEntity, chainData);
        if (serverWorld.spawnEntity(c)) {
            return c;
        } else {
            ConnectibleChains.LOGGER.warn("Tried to summon collision entity for a chain, failed to do so");
            return null;
        }
    }

    public static void destroyCollision(ServerWorld world, Chainable.ChainData chainData) {
        for (Integer entityId : chainData.collisionStorage) {
            Entity e = world.getEntityById(entityId);
            if (e instanceof ChainCollisionEntity) {
                e.discard();
            } else if (e != null) {
                // Ignore null
                ConnectibleChains.LOGGER.warn("Collision storage contained reference to {} (#{}) which is not a collision entity.", e, entityId);
            }
        }
        chainData.collisionStorage.clear();
    }

    public @Nullable Chainable.ChainData getLink() {
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
        if (player != null && player.isHolding(itemStack -> itemStack.isIn(ConventionalItemTags.SHEARS))) {
            return super.shouldRender(distance);
        } else {
            return false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        if (this.link == null || !this.link.isAlive()) {
            this.discard();
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean shouldSave() {
        return false;
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
        if (!super.handleAttack(attacker))
            playSound(getSourceBlockSoundGroup(getLinkSourceItem()).getHitSound(), 0.5F, 1.0F);
        return false;
    }

    /**
     * @see ChainKnotEntity#damage
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient) return false;

        if (source.getAttacker() instanceof PlayerEntity player) {
            boolean isCreative = player.isCreative();
            boolean hasShears = player.getMainHandStack().isIn(ConventionalItemTags.SHEARS);
            if (!isCreative && !hasShears) {
                return false;
            }
        }

        // SEVER-SIDE //
        if (getLink() == null) {
            this.discard();
            return false;
        }

        ActionResult result = onDamageFrom(source, getSourceBlockSoundGroup(getLinkSourceItem()).getHitSound());

        if (!result.isAccepted()) {
            return false;
        }

        if (chainedEntity instanceof Chainable chainable) {
            ConnectibleChains.LOGGER.debug("Dropping chain ({}) due to receiving damage from source: {}", getLink(), source);
            chainable.detachChain(getLink());
        }
        return true;

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
        if (player.getStackInHand(hand).isIn(ConventionalItemTags.SHEARS)) {
            return ActionResult.PASS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        int id = Registries.ITEM.getRawId(linkSourceItem);
        return new EntitySpawnS2CPacket(this, id);
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        int rawChainItemSourceId = packet.getEntityData();
        linkSourceItem = Registries.ITEM.get(rawChainItemSourceId);
    }

    @Override
    public @Nullable ItemStack getPickBlockStack() {
        return new ItemStack(linkSourceItem);
    }
}