package com.github.legoatoom.connectiblechains.networking.packet;

import com.github.legoatoom.connectiblechains.chain.ChainLink;
import com.github.legoatoom.connectiblechains.chain.IncompleteChainLink;
import com.github.legoatoom.connectiblechains.entity.ChainKnotEntity;
import com.github.legoatoom.connectiblechains.util.Helper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;

import static com.github.legoatoom.connectiblechains.ConnectibleChains.LOGGER;

public record ChainAttachPacket(int primaryEntityId, int secondaryEntityId, int chainTypeId,
                                boolean attach) implements FabricPacket {
    public static final PacketType<ChainAttachPacket> TYPE = PacketType.create(
            Helper.identifier("s2c_chain_attach_packet_id"), ChainAttachPacket::new
    );

    /**
     * Links where this is the primary and the secondary doesn't yet exist / hasn't yet loaded.
     * They are kept in a separate list to prevent accidental accesses of the secondary which would
     * result in a NPE. The links will try to be completed each world tick.
     */
    public static final ObjectList<IncompleteChainLink> incompleteLinks = new ObjectArrayList<>(256);


    public ChainAttachPacket(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public ChainAttachPacket(ChainLink link, boolean attach) {
        this(link.getPrimary().getId(), link.getSecondary().getId(), Registries.ITEM.getRawId(link.getSourceItem()), attach);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(primaryEntityId);
        buf.writeInt(secondaryEntityId);
        buf.writeInt(chainTypeId);
        buf.writeBoolean(attach);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    @Environment(EnvType.CLIENT)
    public void apply(ClientPlayerEntity clientPlayerEntity, PacketSender packetSender) {
        if (attach) {
            applyAttach(clientPlayerEntity, packetSender);
            return;
        }
        applyDetach(clientPlayerEntity, packetSender);
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
}
