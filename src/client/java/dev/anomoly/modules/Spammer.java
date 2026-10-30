package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class Spammer extends Module {
    private final StringSetting message = add(new StringSetting("message", "Message", "Anomoly"));
    private final FloatSetting delay = add(new FloatSetting("delay_seconds", "Delay seconds", 5, 2, 120));
    private int ticks;

    public Spammer() {
        super("spammer", "Spammer", "Send a configured chat message on a repeating timer (also covers Spam).", Category.MISC);
    }

    @Override public void tick(Minecraft client) {
        if (message.get().isBlank() || ++ticks < delay.get() * 20) return;
        ticks = 0;
        String text = message.get();
        if (text.startsWith("/")) client.player.connection.sendCommand(text.substring(1));
        else client.player.connection.sendChat(text);
    }

    @Override protected void onEnable(Minecraft client) { ticks = 0; }
}
