package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class Greeter extends Module {
    private final StringSetting greeting = add(new StringSetting("greeting", "Greeting ({player} is replaced)", "Welcome, {player}!"));
    private final Set<UUID> known = new HashSet<>();
    private boolean seeded;

    public Greeter() {
        super("greeter", "Greeter", "Greet each player who joins the server's player list after you.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { known.clear(); seeded = false; }

    @Override public void tick(Minecraft client) {
        Set<UUID> current = new HashSet<>();
        for (PlayerInfo info : client.player.connection.getOnlinePlayers()) {
            UUID id = info.getProfile().id();
            current.add(id);
            if (seeded && !known.contains(id) && !id.equals(client.player.getUUID())) {
                client.player.connection.sendChat(greeting.get().replace("{player}", info.getProfile().name()));
            }
        }
        known.clear();
        known.addAll(current);
        seeded = true;
    }
}
