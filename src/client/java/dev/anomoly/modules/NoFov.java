package dev.anomoly.modules;

import dev.anomoly.Module;

public final class NoFov extends Module {
    public NoFov() {
        super("no_fov", "No FOV", "Keep the configured FOV instead of applying sprint and item modifiers.", Category.RENDER);
    }
}
