package com.github.legoatoom.connectiblechains.util;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import net.minecraft.util.Identifier;

public class Helper {

    public static Identifier identifier(String name){
        return new Identifier(ConnectibleChains.MODID, name);
    }
}
