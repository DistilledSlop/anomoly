package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AutoAccept extends Module implements ChatFilter {
    private final StringSetting players = add(new StringSetting("players", "Accept only from (comma separated names)", ""));
    private final StringSetting trigger = add(new StringSetting("trigger", "Request text contains", "teleport to you"));
    private final StringSetting command = add(new StringSetting("command", "Command to send", "tpaccept"));

    public AutoAccept() {
        super("auto_accept", "AutoAccept", "Send an accept command when a listed player asks to teleport to you.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) {
        String text = message.getString();
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.contains(trigger.get().toLowerCase(Locale.ROOT))) return message;
        for (String name : players.get().split(",")) {
            String trimmed = name.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty() && lower.contains(trimmed)) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> { if (client.player != null) client.player.connection.sendCommand(command.get()); });
                break;
            }
        }
        return message;
    }
}
