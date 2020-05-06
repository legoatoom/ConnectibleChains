package com.legoatoom.develop.network.packet.s2c.play;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;

import java.io.IOException;

public class EntitiesAttachS2CPacket extends EntityAttachS2CPacket {
    private int attachedId;
    private int limboId;
    private int fromId;
    private boolean remove;


    public EntitiesAttachS2CPacket(Entity attackEntity, int limboEntity, boolean remove, int fromId){
        this.attachedId = attackEntity.getEntityId();
        this.limboId = limboEntity;
        this.remove = remove;
        this.fromId = fromId;
    }


    @Override
    public void read(PacketByteBuf buf) throws IOException {
        this.attachedId = buf.readInt();
        this.limboId = buf.readInt();
        this.remove = buf.readBoolean();
        this.fromId = buf.readInt();
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        buf.writeInt(this.attachedId);
        buf.writeInt(this.limboId);
        buf.writeBoolean(this.remove);
        buf.writeInt(this.fromId);
    }

    @Override
    public void apply(ClientPlayPacketListener clientPlayPacketListener) {
        clientPlayPacketListener.onEntityAttach(this);
    }

    @Environment(EnvType.CLIENT)
    public int getAttachedEntityId() {
        return this.attachedId;
    }

    @Environment(EnvType.CLIENT)
    public int getLimboId() { return this.limboId;}

    @Environment(EnvType.CLIENT)
    public boolean isRemove() { return this.remove;}

    @Environment(EnvType.CLIENT)
    public int getFromId() { return this.fromId;}
}
