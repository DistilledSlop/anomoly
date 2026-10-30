package dev.anomoly.modules;

import dev.anomoly.Module;

public final class NoSlowBreak extends Module {
    public NoSlowBreak() {
        super("no_slow_break", "NoSlowBreak", "Remove the mining-speed penalties for being in the air and for being underwater.", Category.WORLD);
    }
}
