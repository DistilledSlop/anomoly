package dev.anomoly.modules;

import dev.anomoly.Module;

public final class SafeWalk extends Module {
    public SafeWalk() {
        super("safe_walk", "Safe Walk", "Use vanilla edge clipping without sneak movement slowdown.", Category.MOVEMENT);
    }
}
