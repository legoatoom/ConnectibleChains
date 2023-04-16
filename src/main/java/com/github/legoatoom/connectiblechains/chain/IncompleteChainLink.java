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

import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;

/**
 * Due to the asynchronous nature of networking an attach- or detach-packet cann arrive before the secondary exists.
 * This class acts as a temporary storage until the real link can be created.
 *
 * @author Qendolin
 */
@Environment(EnvType.CLIENT)
public class IncompleteChainLink {
    /**
     * @see ChainLink#primary
     */
    public final ChainKnotEntity primary;
    /**
     * @see ChainLink#primary
     */
    public final int secondaryId;

    public final Item sourceItem;
    /**
     * Whether the link exists and is active
     */
    private boolean alive = true;

    public IncompleteChainLink(ChainKnotEntity primary, int secondaryId, Item sourceItem) {
        this.primary = primary;
        this.secondaryId = secondaryId;
        this.sourceItem = sourceItem;
    }

    /**
     * Tries to complete the chain link by looking for an entity with {@link #secondaryId}.
     *
     * @return true if the incomplete chain link should be removed
     */
    public boolean tryCompleteOrRemove() {
        if (isDead()) return true;
        Entity secondary = primary.world.getEntityById(secondaryId);
        if (secondary == null) return false;
        ChainLink.create(primary, secondary, sourceItem);
        return true;
    }

    public boolean isDead() {
        return !alive || this.primary.isRemoved();
    }

    /**
     * Sometimes the detach-packed can be received before the secondary exists
     * so even incomplete links can be destroyed.
     */
    public void destroy() {
        if (!alive) return;
        this.alive = false;
        // Can't drop items on the client I guess
    }
}
