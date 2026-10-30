package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class TpsCounter extends TextHud {
    public TpsCounter() {
        super("tps_counter", "TPSCounter", "Estimate the server's ticks per second from the spacing of its world-time updates.");
    }

    @Override protected List<String> lines(Minecraft client) {
        return List.of(String.format(Locale.ROOT, "TPS %.1f", ServerClock.tps()));
    }
}
