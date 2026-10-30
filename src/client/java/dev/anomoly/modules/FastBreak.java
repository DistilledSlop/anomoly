package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MultiPlayerGameModeAccessor;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

public final class FastBreak extends Module {
    private final FloatSetting multiplier = add(new FloatSetting("multiplier", "Mining speed", 1.25F, 1.0F, 3.0F));

    public FastBreak() {
        super("fast_break", "Fast Break", "Increase mining progress and remove the local block-break interval.", Category.WORLD);
    }

    public float multiplier() { return multiplier.get(); }

    @Override public void tick(Minecraft client) {
        if (client.gameMode != null) ((MultiPlayerGameModeAccessor) client.gameMode).anomoly$setDestroyDelay(0);
    }
}
