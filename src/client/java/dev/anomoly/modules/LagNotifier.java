package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class LagNotifier extends TextHud {
    public LagNotifier() {
        super("lag_notifier", "LagNotifier", "Warn on the HUD when the server has sent nothing for over a second (also covers LagDetector).");
    }

    @Override protected List<String> lines(Minecraft client) {
        double silence = ServerClock.secondsSincePacket();
        return silence < 1.0 ? List.of() : List.of(String.format(Locale.ROOT, "Server silent for %.1fs", silence));
    }
}
