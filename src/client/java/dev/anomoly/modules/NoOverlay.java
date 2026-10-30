package dev.anomoly.modules;

import dev.anomoly.Module;

public final class NoOverlay extends Module {
    public NoOverlay() {
        super("no_overlay", "No Overlay", "Hide water and view-blocking screen overlays.", Category.RENDER);
    }
}
