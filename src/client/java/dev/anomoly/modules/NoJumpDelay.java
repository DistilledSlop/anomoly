package dev.anomoly.modules;

import dev.anomoly.Module;

/** Removes the local ten-tick gate between held-key ground jumps. */
public final class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super("no_jump_delay", "NoJumpDelay", "Jump again as soon as you land while holding jump.", Category.MOVEMENT);
    }
}
