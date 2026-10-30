package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class TimerIndicator extends TextHud {
    public TimerIndicator() {
        super("timer_indicator", "TimerIndicator", "Show the current Timer module speed multiplier on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        return List.of(String.format(Locale.ROOT, "Timer x%.2f", AnomolyClient.MODULES.get(Timer.class).factor()));
    }
}
