package dev.anomoly.modules;

import dev.anomoly.Module;

public final class PlayerRadar extends Module {
    public PlayerRadar() {
        super("player_radar", "Player Radar", "List players currently loaded by the client in the ClickGUI.", Category.RENDER);
    }
}
