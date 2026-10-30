package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Notifications extends Module {
    private final Map<Module, Boolean> last = new HashMap<>();

    public Notifications() {
        super("notifications", "Notifications", "Show a brief action-bar message whenever another module is switched on or off.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) {
        last.clear();
        for (Module module : AnomolyClient.MODULES.all()) last.put(module, module.enabled());
    }

    @Override public void tick(Minecraft client) {
        for (Module module : AnomolyClient.MODULES.all()) {
            boolean now = module.enabled();
            Boolean before = last.put(module, now);
            if (before != null && before != now && module != this) {
                client.player.sendOverlayMessage(Component.literal(module.name() + (now ? " enabled" : " disabled")));
            }
        }
    }
}
