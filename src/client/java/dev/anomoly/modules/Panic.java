package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class Panic extends Module {
    public Panic() {
        super("panic", "Panic", "One-shot: instantly switch off every enabled module, then switch itself off (also covers Self Destruct).", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) {
        AnomolyClient.MODULES.disableAll();
    }
}
