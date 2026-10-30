package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class Step extends Module {
    private final FloatSetting height = add(new FloatSetting("height", "Step height", 1.0F, 0.6F, 2.5F));

    public Step() {
        super("step", "Step", "Walk up taller blocks using the client collision step path.", Category.MOVEMENT);
    }

    public float height() { return height.get(); }
}
