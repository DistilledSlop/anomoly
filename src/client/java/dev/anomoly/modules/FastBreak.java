package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;

public final class FastBreak extends Module {
    public FastBreak() {
        super("fast_break", "Fast Break", "Remove the local block-break interval.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode != null) ((MultiPlayerGameModeAccessor) client.gameMode).anomoly$setDestroyDelay(0);
    }
}
