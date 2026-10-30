package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class TimeChanger extends Module {
    private final FloatSetting time = add(new FloatSetting("time", "Time of day", 6000, 0, 23999));
    private final FloatSetting moon = add(new FloatSetting("moon", "Moon phase (0-7)", 0, 0, 7));

    public TimeChanger() {
        super("time_changer", "Time Changer", "Set local overworld time and moon phase without changing the server clock.", Category.RENDER);
    }

    public long fixedTicks() {
        return Math.round(moon.get()) * 24000L + Math.round(time.get());
    }
}
