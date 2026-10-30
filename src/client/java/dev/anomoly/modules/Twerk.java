package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class Twerk extends Module {
    private final FloatSetting period = add(new FloatSetting("period_ticks", "Cycle ticks", 8, 4, 40));
    private int ticks;

    public Twerk() {
        super("twerk", "Twerk", "Crouch and stand on a repeating timer.", Category.FUN);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null) { client.options.keyShift.setDown(false); return; }
        ticks = (ticks + 1) % Math.max(4, (int)period.get());
        client.options.keyShift.setDown(ticks < (int)period.get() / 2);
    }

    @Override protected void onDisable(Minecraft client) {
        ticks = 0;
        KeyMapping.setAll();
    }
}
