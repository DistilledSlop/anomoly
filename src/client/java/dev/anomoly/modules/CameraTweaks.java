package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class CameraTweaks extends Module {
    private final FloatSetting distance = add(new FloatSetting("distance", "Third-person distance", 4, 1, 12));

    public CameraTweaks() {
        super("camera_tweaks", "Camera Tweaks", "Adjust the third-person camera distance.", Category.RENDER);
    }

    public float distanceFactor() { return distance.get() / 4.0F; }
}
