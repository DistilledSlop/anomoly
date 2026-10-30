package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

public final class HandDerp extends Module {
    private final IntSetting interval = add(new IntSetting("interval_ticks", "Ticks between flips", 2, 1, 20));
    private HumanoidArm original;

    public HandDerp() {
        super("hand_derp", "HandDerp", "Flip your main hand between left and right so your held item jitters for other players.", Category.FUN);
    }

    @Override protected void onEnable(Minecraft client) { original = client.options.mainHand().get(); }

    @Override public void tick(Minecraft client) {
        if (client.player.tickCount % interval.get() != 0) return;
        client.options.mainHand().set(client.options.mainHand().get() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
    }

    @Override protected void onDisable(Minecraft client) {
        if (original != null) client.options.mainHand().set(original);
    }
}
