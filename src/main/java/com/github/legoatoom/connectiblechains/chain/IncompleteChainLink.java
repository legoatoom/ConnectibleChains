package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;

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
    /**
     * @see ChainLink#chainType
     */
    public final ChainType chainType;
    /**
     * Whether the link exists and is active
     */
    private boolean alive = true;

    public IncompleteChainLink(ChainKnotEntity primary, int secondaryId, ChainType chainType) {
        this.primary = primary;
        this.secondaryId = secondaryId;
        this.chainType = chainType;
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
        ChainLink.create(primary, secondary, chainType);
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
