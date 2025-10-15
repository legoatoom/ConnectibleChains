/*
 * Copyright (C) 2025 legoatoom
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.legoatoom.connectiblechains.entity;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;

/**
 * ChainLinkEntity implements common functionality between {@link ChainCollisionEntity} and {@link ChainKnotEntity}.
 */
public interface ChainLinkEntity {
    private static <E extends Entity & ChainLinkEntity> ActionResult onDamageFrom(E self, DamageSource source, SoundEvent hitSound) {
        if (self.getEntityWorld().isClient()) {
            return ActionResult.PASS;
        }
        // SERVER-SIDE //
        if (self.isInvulnerable()) {
            return ActionResult.FAIL;
        }

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            return ActionResult.SUCCESS;
        }

        if (source.getWeaponStack() != null && source.getWeaponStack().isIn(ConventionalItemTags.SHEAR_TOOLS)) {
            return ActionResult.SUCCESS;
        }

        if (!source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            // Projectiles such as arrows (actually probably just arrows) can get "stuck"
            // on entities they cannot damage, such as players while blocking with shields or these chains.
            // That would cause some serious sound spam, and we want to avoid that.

            self.playSound(hitSound, 0.5F, 1.0F);
        }
        return ActionResult.FAIL;
    }

    /**
     * When a chain link entity is damaged by
     * <ul>
     * <li>A player with an item that has the tag c:shears or is minecraft:shears</li>
     * <li>An explosion</li>
     * </ul>
     * it destroys the link that it is part of.
     * Otherwise, it plays a hit sound.
     *
     * @param source The source that was used to damage.
     * @return {@link ActionResult#SUCCESS} when the link should be destroyed,
     * {@link ActionResult#CONSUME} when the link should be destroyed but not drop.
     */
    default ActionResult onDamageFrom(DamageSource source, SoundEvent hitSound) {
        return onDamageFrom((Entity & ChainLinkEntity) this, source, hitSound);
    }
}
