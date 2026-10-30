package dev.anomoly.modules;

import dev.anomoly.Module;

/** Mirrors the empty upstream registration: the source snapshot implements no behavior. */
public final class AntiBallPlace extends Module {
    public AntiBallPlace() {
        super("anti_ball_place", "AntiBallPlace", "Placeholder matching the empty upstream module; it does nothing.", Category.PLAYER);
    }
}
