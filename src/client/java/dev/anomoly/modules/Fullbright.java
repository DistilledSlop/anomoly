package dev.anomoly.modules;

import dev.anomoly.Module;

public final class Fullbright extends Module {
    public Fullbright() {
        super("fullbright", "Fullbright", "Render the world with a fully lit lightmap regardless of block and sky light.", Category.RENDER);
    }
}
