package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;

public final class FastPlace extends Module {
    public FastPlace() {
        super("fast_place", "Fast Place", "Remove the client right-click interval.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        ((MinecraftAccessor) client).anomoly$setRightClickDelay(0);
    }
}
