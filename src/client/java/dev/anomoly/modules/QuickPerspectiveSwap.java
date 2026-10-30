package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class QuickPerspectiveSwap extends Module {
    private CameraType previous;

    public QuickPerspectiveSwap() {
        super("quick_perspective_swap", "QuickPerspectiveSwap", "While enabled, show the front-facing third-person camera; bind it to a key and hold or tap to flip back and forth.", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) {
        previous = client.options.getCameraType();
        client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }

    @Override protected void onDisable(Minecraft client) {
        if (previous != null) client.options.setCameraType(previous);
        previous = null;
    }
}
