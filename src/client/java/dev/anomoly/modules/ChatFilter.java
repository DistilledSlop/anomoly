package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Implemented by modules that inspect, rewrite, or hide chat lines before they are shown. */
public interface ChatFilter {
    /** Returns the line to display, or null to hide it. */
    Component onIncoming(Component message);

    /** Returns the text to send in place of what the player typed. */
    default String onOutgoing(String message) { return message; }

    static Component incoming(Component message) {
        if (Minecraft.getInstance().player == null) return message;
        Component current = message;
        for (Module module : AnomolyClient.MODULES.all()) {
            if (current != null && module.enabled() && module instanceof ChatFilter filter) current = filter.onIncoming(current);
        }
        return current;
    }

    static String outgoing(String message) {
        String current = message;
        for (Module module : AnomolyClient.MODULES.all()) {
            if (module.enabled() && module instanceof ChatFilter filter) current = filter.onOutgoing(current);
        }
        return current;
    }
}
