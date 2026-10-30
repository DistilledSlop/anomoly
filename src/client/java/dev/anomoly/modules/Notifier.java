package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class Notifier extends Module {
    private final BooleanSetting joins = add(new BooleanSetting("joins", "Player joins and leaves", true));
    private final Set<UUID> known = new HashSet<>();
    private boolean seeded;

    public Notifier() {
        super("notifier", "Notifier", "Post a local chat note when players join or leave the server.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { known.clear(); seeded = false; }

    @Override public void tick(Minecraft client) {
        if (!joins.get()) return;
        Set<UUID> now = new HashSet<>();
        for (PlayerInfo info : client.player.connection.getOnlinePlayers()) {
            now.add(info.getProfile().id());
            if (seeded && !known.contains(info.getProfile().id())) {
                client.player.sendSystemMessage(Component.literal("[Anomoly] " + info.getProfile().name() + " joined."));
            }
        }
        if (seeded) for (UUID id : known) {
            if (!now.contains(id)) client.player.sendSystemMessage(Component.literal("[Anomoly] A player left the server."));
        }
        known.clear();
        known.addAll(now);
        seeded = true;
    }
}
