package dev.anomoly.modules;

import dev.anomoly.Module;

public final class NoFog extends Module {
    public NoFog() {
        super("no_fog", "No Fog", "Move distance and environmental fog beyond visible terrain.", Category.RENDER);
    }
}
