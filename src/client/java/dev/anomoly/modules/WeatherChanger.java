package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;

public final class WeatherChanger extends Module {
    private final FloatSetting rain = add(new FloatSetting("rain", "Rain intensity", 0, 0, 1));
    private final FloatSetting thunder = add(new FloatSetting("thunder", "Thunder intensity", 0, 0, 1));

    public WeatherChanger() {
        super("weather_changer", "Weather Changer", "Override local rain and thunder; zero gives clear weather.", Category.RENDER);
    }

    public float rain() { return rain.get(); }
    public float thunder() { return thunder.get() * rain.get(); }
}
