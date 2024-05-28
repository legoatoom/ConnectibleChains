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

package com.github.legoatoom.connectiblechains.networking.packet;

import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.IncompleteChainLink;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;

import static com.github.legoatoom.connectiblechains.ConnectibleChains.LOGGER;

public record ChainAttachPayload(int primaryEntityId, int secondaryEntityId, int chainTypeId,
                                 boolean attach) implements CustomPayload {
    public static final CustomPayload.Id<ChainAttachPayload> PAYLOAD_ID = new CustomPayload.Id<>(Helper.identifier("s2c_chain_attach_packet_id"));
    public static final PacketCodec<RegistryByteBuf, ChainAttachPayload> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, ChainAttachPayload::primaryEntityId,
                    PacketCodecs.INTEGER, ChainAttachPayload::secondaryEntityId,
                    PacketCodecs.INTEGER, ChainAttachPayload::chainTypeId,
                    PacketCodecs.BOOL, ChainAttachPayload::attach,
                    ChainAttachPayload::new);

    /**
     * Links where this is the primary and the secondary doesn't yet exist / hasn't yet loaded.
     * They are kept in a separate list to prevent accidental accesses of the secondary which would
     * result in a NPE. The links will try to be completed each world tick.
     */
    public static final ObjectList<IncompleteChainLink> incompleteLinks = new ObjectArrayList<>(256);


    public ChainAttachPayload(ChainLink link, boolean attach) {
        this(link.getPrimary().getId(), link.getSecondary().getId(), Registries.ITEM.getRawId(link.getSourceItem()), attach);
    }

    public static void encode(PacketByteBuf buf1, ChainAttachPayload packet) {
        buf1.writeInt(packet.primaryEntityId());
        buf1.writeInt(packet.secondaryEntityId());
        buf1.writeInt(packet.chainTypeId());
        buf1.writeBoolean(packet.attach());
    }


    private void applyDetach(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
        ClientWorld world = clientPlayerEntity.clientWorld;
        Entity primary = world.getEntityById(primaryEntityId);

        if (!(primary instanceof ChainKnotEntity primaryKnot)) {
            LOGGER.warn(String.format("Tried to detach from %s (#%d) which is not a chain knot",
                    primary, primaryEntityId
            ));
            return;
        }
        Entity secondary = world.getEntityById(secondaryEntityId);
        incompleteLinks.removeIf(link -> {
            if (link.primary == primaryKnot && link.secondaryId == secondaryEntityId) {
                link.destroy();
                return true;
            }
            return false;
        });

        if (secondary == null) {
            return;
        }

        for (ChainLink link : primaryKnot.getLinks()) {
            if (link.getSecondary() == secondary) {
                link.destroy(true);
            }
        }
    }

    private void applyAttach(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
        ClientWorld world = clientPlayerEntity.clientWorld;
        Entity primary = world.getEntityById(primaryEntityId);

        if (!(primary instanceof ChainKnotEntity primaryKnot)) {
            LOGGER.warn(String.format("Tried to attach from %s (#%d) which is not a chain knot",
                    primary, primaryEntityId
            ));
            return;
        }
        Entity secondary = world.getEntityById(secondaryEntityId);

        Item chainType = Registries.ITEM.get(chainTypeId);

        if (secondary == null) {
            incompleteLinks.add(new IncompleteChainLink(primaryKnot, secondaryEntityId, chainType));
        } else {
            ChainLink.create(primaryKnot, secondary, chainType);
        }
    }

    @Override
    public Id<ChainAttachPayload> getId() {
        return PAYLOAD_ID;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayNetworking.Context context) {
        if (attach){
            applyAttach(context.player(), context.responseSender());
            return;
        }
        applyDetach(context.player(), context.responseSender());
    }
}
