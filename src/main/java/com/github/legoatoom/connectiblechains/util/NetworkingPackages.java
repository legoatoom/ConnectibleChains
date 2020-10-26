package com.github.legoatoom.connectiblechains.util;

import net.minecraft.util.Identifier;

public class NetworkingPackages {

    // Id for sending chain connection updates over the network.
    public static final Identifier S2C_CHAIN_ATTACH_PACKET_ID = Helper.identifier("s2c_chain_attach_packet_id");
    public static final Identifier S2C_CHAIN_DETACH_PACKET_ID = Helper.identifier("s2c_chain_detach_packet_id");

    public static final Identifier S2C_MULTI_CHAIN_ATTACH_PACKET_ID = Helper.identifier("s2c_multi_chain_attach_packet_id");
    public static final Identifier S2C_MULTI_CHAIN_DETACH_PACKET_ID = Helper.identifier("s2c_multi_chain_detach_packet_id");
}
