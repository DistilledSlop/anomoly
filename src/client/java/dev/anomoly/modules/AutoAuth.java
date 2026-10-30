package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AutoAuth extends Module implements ChatFilter {
    private final StringSetting password = add(new StringSetting("password", "Password (stored unencrypted in the config)", ""));
    private long lastSent;

    public AutoAuth() {
        super("auto_auth", "AutoAuth", "Answer a server's /login or /register prompt with the saved password (also covers AutoAccount).", Category.MISC);
    }

    @Override public Component onIncoming(Component message) {
        if (password.get().isBlank() || System.currentTimeMillis() - lastSent < 5000) return message;
        String text = message.getString().toLowerCase(Locale.ROOT);
        String command = text.contains("/register") ? "register " + password.get() + " " + password.get()
            : text.contains("/login") ? "login " + password.get() : null;
        if (command == null) return message;
        lastSent = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> { if (client.player != null) client.player.connection.sendCommand(command); });
        return message;
    }
}
