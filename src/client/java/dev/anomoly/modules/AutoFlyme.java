package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class AutoFlyme extends Module {
    private final StringSetting command = add(new StringSetting("command", "Command (no slash)", "flyme"));
    private final IntSetting seconds = add(new IntSetting("seconds", "Seconds between sends", 30, 5, 600));
    private int ticks;

    public AutoFlyme() {
        super("auto_flyme", "AutoFlyme", "Send a configured server command, /flyme by default, on a repeating timer.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { ticks = 0; }

    @Override public void tick(Minecraft client) {
        if (ticks++ % (seconds.get() * 20) != 0 || command.get().isBlank()) return;
        client.player.connection.sendCommand(command.get());
    }
}
