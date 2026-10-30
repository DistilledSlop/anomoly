package dev.anomoly.modules;

import dev.anomoly.Module;

/** Include fluid surfaces in the player's normal block-target raycast. */
public final class LiquidInteract extends Module {
    public LiquidInteract() {
        super("liquid_interact", "LiquidInteract",
            "Target water and lava surfaces with normal block interactions.", Category.PLAYER);
    }
}
