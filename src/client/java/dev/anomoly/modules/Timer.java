package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class Timer extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Game speed", 1.2F, 0.1F, 5.0F));

    public Timer() {
        super("timer", "Timer", "Run the client game loop faster or slower than normal; the server sees the changed packet rate.", Category.WORLD);
    }

    public float factor() { return enabled() ? speed.get() : 1.0F; }
}
