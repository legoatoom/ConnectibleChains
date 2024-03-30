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

package com.github.legoatoom.connectiblechains.client;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import com.github.legoatoom.connectiblechains.chain.IncompleteChainLink;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.networking.packet.ChainAttachPacket;
import com.github.legoatoom.connectiblechains.networking.packet.MultiChainAttachPacket;
import com.github.legoatoom.connectiblechains.util.NetworkingPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import static net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver;

@Environment(EnvType.CLIENT)
public class ChainPacketHandler {

    public ChainPacketHandler() {
        register();
    }

    private void register() {
        registerGlobalReceiver(ChainAttachPacket.TYPE, ChainAttachPacket::apply);
        registerGlobalReceiver(MultiChainAttachPacket.TYPE, MultiChainAttachPacket::apply);
        registerGlobalReceiver(NetworkingPackets.S2C_KNOT_CHANGE_TYPE_PACKET, (client, handler, buf, responseSender) -> {
            int knotId = buf.readVarInt();
            int typeId = buf.readVarInt();

            client.execute(() -> {
                if (client.world == null) return;
                Entity entity = client.world.getEntityById(knotId);
                Item chainType = Registries.ITEM.get(typeId);
                if (entity instanceof ChainKnotEntity knot) {
                    knot.updateChainType(chainType);
                } else {
                    logBadActionTarget("change type of", entity, knotId, "chain knot");
                }
            });
        });
    }

    private void logBadActionTarget(String action, Entity target, int targetId, String expectedTarget) {
        ConnectibleChains.LOGGER.warn(String.format("Tried to %s %s (#%d) which is not %s",
                action, target, targetId, expectedTarget
        ));
    }

    /**
     * Called on every client tick.
     * Tries to complete all links.
     * Completed links or links that are no longer valid because the primary is dead are removed.
     */
    public void tick() {
        ChainAttachPacket.incompleteLinks.removeIf(IncompleteChainLink::tryCompleteOrRemove);
    }
}
