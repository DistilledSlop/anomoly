package dev.anomoly.modules;

import dev.anomoly.Module;

public final class AntiLegitMiss extends Module {
    public AntiLegitMiss() {
        super("anti_legit_miss", "AntiLegitMiss", "Cancel an attack click that is not aimed at an entity or block, so misses do not swing.", Category.COMBAT);
    }
}
