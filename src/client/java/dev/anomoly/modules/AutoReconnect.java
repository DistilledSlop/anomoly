package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class AutoReconnect extends Module {
    private final FloatSetting delaySeconds = add(new FloatSetting("delay_seconds", "Delay seconds", 10, 1, 120));
    private final FloatSetting maxAttempts = add(new FloatSetting("max_attempts", "Maximum attempts", 3, 1, 10));
    private ServerData server;
    private Screen disconnectScreen;
    private long screenSince;
    private int attempts;
    private boolean attemptedThisScreen;

    public AutoReconnect() {
        super("auto_reconnect", "Auto Reconnect", "Retry a disconnected multiplayer server after a delay.", Category.MISC);
    }

    @Override public void tick(Minecraft client) {
        ServerData current = client.getCurrentServer();
        if (current != null && !client.isLocalServer()) {
            server = current;
            attempts = 0;
            disconnectScreen = null;
            attemptedThisScreen = false;
        }
    }

    public void tickDisconnected(Minecraft client) {
        if (!enabled() || server == null) return;
        Screen screen = client.gui.screen();
        if (!(screen instanceof DisconnectedScreen)) {
            disconnectScreen = null;
            attemptedThisScreen = false;
            return;
        }
        if (screen != disconnectScreen) {
            disconnectScreen = screen;
            screenSince = System.nanoTime();
            attemptedThisScreen = false;
        }
        if (attemptedThisScreen || attempts >= (int)maxAttempts.get()
            || System.nanoTime() - screenSince < (long)(delaySeconds.get() * 1_000_000_000L)) return;
        attemptedThisScreen = true;
        attempts++;
        ConnectScreen.startConnecting(screen, client, ServerAddress.parseString(server.ip), server, false, null);
    }

    @Override protected void onEnable(Minecraft client) {
        server = null;
        disconnectScreen = null;
        attempts = 0;
        attemptedThisScreen = false;
    }

    @Override protected void onDisable(Minecraft client) {
        server = null;
        disconnectScreen = null;
        attempts = 0;
        attemptedThisScreen = false;
    }
}
