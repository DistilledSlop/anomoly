package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class AutoF5 extends Module {
    private CameraType previous;

    public AutoF5() {
        super("auto_f5", "Auto F5", "Switch to rear third person in container screens, then restore the view.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() instanceof AbstractContainerScreen<?>) {
            if (previous == null) {
                previous = client.options.getCameraType();
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else restore(client);
    }

    private void restore(Minecraft client) {
        if (previous == null) return;
        if (client.options.getCameraType() == CameraType.THIRD_PERSON_BACK) client.options.setCameraType(previous);
        previous = null;
    }

    @Override protected void onDisable(Minecraft client) { restore(client); }
}
