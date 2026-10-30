package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class FovOverride extends Module {
    private final FloatSetting degrees = add(new FloatSetting("degrees", "FOV degrees", 90, 30, 140));

    public FovOverride() {
        super("fov_override", "FOV Modifier", "Use a fixed camera field of view.", Category.RENDER);
    }

    public float degrees() { return degrees.get(); }
}
