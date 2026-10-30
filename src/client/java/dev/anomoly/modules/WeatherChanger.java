package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;

public final class WeatherChanger extends Module {
    private final FloatSetting rain = add(new FloatSetting("rain", "Rain intensity", 0, 0, 1));
    private final FloatSetting thunder = add(new FloatSetting("thunder", "Thunder intensity", 0, 0, 1));
    private final BooleanSetting customTime = add(new BooleanSetting("custom_time", "Override time and moon", false));
    private final FloatSetting time = add(new FloatSetting("time", "Time of day", 6000, 0, 23999));
    private final FloatSetting moon = add(new FloatSetting("moon", "Moon phase (0-7)", 0, 0, 7));

    public WeatherChanger() {
        super("weather_changer", "Weather Changer", "Override local weather, with optional time and moon controls.", Category.RENDER);
    }

    public float rain() { return rain.get(); }
    public float thunder() { return thunder.get() * rain.get(); }
    public boolean overridesTime() { return enabled() && customTime.get(); }
    public long fixedTicks() { return Math.round(moon.get()) * 24000L + Math.round(time.get()); }
}
