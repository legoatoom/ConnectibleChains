package com.github.legoatoom.connectiblechains.chain;

import com.github.legoatoom.connectiblechains.enitity.ChainKnotEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public class IncompleteChainLink {
    public final ChainKnotEntity primary;
    public final int secondaryId;
    public final ChainType chainType;
    private boolean alive = true;

    public IncompleteChainLink(ChainKnotEntity primary, int secondaryId, ChainType chainType) {
        this.primary = primary;
        this.secondaryId = secondaryId;
        this.chainType = chainType;
    }

    public boolean tryCompleteOrRemove() {
        if (isDead()) return true;
        Entity secondary = primary.world.getEntityById(secondaryId);
        if (secondary == null) return false;
        ChainLink.create(primary, secondary, chainType);
        return true;
    }

    public void destroy() {
        if (!alive) return;
        this.alive = false;
        // Can't drop items on the client I guess
    }

    public boolean isDead() {
        return !alive || this.primary.isRemoved();
    }
}
