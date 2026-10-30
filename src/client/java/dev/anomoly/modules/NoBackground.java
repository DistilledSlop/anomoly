package dev.anomoly.modules;

import dev.anomoly.Module;

public final class NoBackground extends Module {
    public NoBackground() {
        super("no_background", "NoBackground", "Remove the dark and blurred backdrop behind inventories and other in-world screens.", Category.RENDER);
    }
}
