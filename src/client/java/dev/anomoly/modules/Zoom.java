package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class Zoom extends Module {
    private final FloatSetting degrees = add(new FloatSetting("degrees", "Zoom FOV", 25, 10, 70));

    public Zoom() {
        super("zoom", "Zoom", "Narrow FOV while the Zoom key is held.", Category.RENDER);
    }

    public float degrees() { return degrees.get(); }
}
