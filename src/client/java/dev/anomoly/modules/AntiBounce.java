package dev.anomoly.modules;

import dev.anomoly.Module;

/** Keep the local player grounded after landing on springy blocks. */
public final class AntiBounce extends Module {
    public AntiBounce() {
        super("anti_bounce", "AntiBounce",
            "Suppress bounce when landing on slime and other springy blocks.", Category.MOVEMENT);
    }
}
