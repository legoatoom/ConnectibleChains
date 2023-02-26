package com.github.legoatoom.connectiblechains.entity;

import com.github.legoatoom.connectiblechains.tag.CommonTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/**
 * ChainLinkEntity implements common functionality between {@link ChainCollisionEntity} and {@link ChainKnotEntity}.
 */
public interface ChainLinkEntity {

    /**
     * When a chain link entity is damaged by
     * <ul>
     * <li>A player with an item that has the tag c:shears or is minecraft:shears</li>
     * <li>An explosion</li>
     * </ul>
     * it destroys the link that it is part of.
     * Otherwise, it plays a hit sound.
     *
     * @param self   A {@link ChainCollisionEntity} or {@link ChainKnotEntity}.
     * @param source The source that was used to damage.
     * @return {@link ActionResult#SUCCESS} when the link should be destroyed,
     * {@link ActionResult#CONSUME} when the link should be destroyed but not drop.
     */
    static ActionResult onDamageFrom(Entity self, DamageSource source) {
        if (self.isInvulnerableTo(source)) {
            return ActionResult.FAIL;
        }
        if (self.world.isClient) {
            return ActionResult.PASS;
        }

        if (source.isExplosive()) {
            return ActionResult.SUCCESS;
        }
        if (source.getSource() instanceof PlayerEntity player) {
            if (canDestroyWith(player.getMainHandStack())) {
                return ActionResult.success(!player.isCreative());
            }
        }

        if (!source.isProjectile()) {
            // Projectiles such as arrows (actually probably just arrows) can get "stuck"
            // on entities they cannot damage, such as players while blocking with shields or these chains.
            // That would cause some serious sound spam, and we want to avoid that.
            self.playSound(SoundEvents.BLOCK_CHAIN_HIT, 0.5F, 1.0F);
        }
        return ActionResult.FAIL;
    }

    /**
     * @param item The item subject of an interaction
     * @return true if a chain link entity can be destroyed with the item
     */
    static boolean canDestroyWith(ItemStack item) {
        return CommonTags.isShear(item);
    }

    /**
     * Destroys all links associated with this entity
     *
     * @param mayDrop true when the links should drop
     */
    void destroyLinks(boolean mayDrop);
}
