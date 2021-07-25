package com.github.legoatoom.connectiblechains.config;

import com.github.legoatoom.connectiblechains.ConnectibleChains;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = ConnectibleChains.MODID)
public class ModConfig implements ConfigData {

    private float chainHangAmount = 9.0F;
    @ConfigEntry.BoundedDiscrete(max = 32)
    @ConfigEntry.Gui.Tooltip()
    private int maxChainRange = 7;

    public float getChainHangAmount() {
        return chainHangAmount;
    }

    @SuppressWarnings("unused")
    public void setChainHangAmount(float chainHangAmount) {
        this.chainHangAmount = chainHangAmount;
    }

    public int getMaxChainRange() {
        return maxChainRange;
    }

    @SuppressWarnings("unused")
    public void setMaxChainRange(int maxChainRange) {
        this.maxChainRange = maxChainRange;
    }
}
