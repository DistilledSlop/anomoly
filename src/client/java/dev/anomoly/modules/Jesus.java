package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.material.FluidState;

public final class Jesus extends Module {
    private final BooleanSetting water = add(new BooleanSetting("water", "Walk on water", true));
    private final BooleanSetting lava = add(new BooleanSetting("lava", "Walk on lava", false));

    public Jesus() {
        super("jesus", "Jesus", "Stand and walk on the surface of fluids; sneak to sink.", Category.MOVEMENT);
    }

    public boolean canStandOn(LocalPlayer player, FluidState fluid) {
        if (!enabled() || player.isShiftKeyDown() || player.isUnderWater()) return false;
        return water.get() && fluid.is(net.minecraft.tags.FluidTags.WATER) || lava.get() && fluid.is(net.minecraft.tags.FluidTags.LAVA);
    }
}
