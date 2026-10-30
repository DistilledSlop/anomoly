package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class MessageAura extends Module {
    private final StringSetting message = add(new StringSetting("message", "Private message", "Hello!"));
    private final Set<UUID> greeted = new HashSet<>();

    public MessageAura() {
        super("message_aura", "Message Aura", "Whisper a message to each player who first enters your render distance.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) {
        greeted.clear();
        for (Player other : client.level.players()) greeted.add(other.getUUID());
    }

    @Override public void tick(Minecraft client) {
        for (Player other : client.level.players()) {
            if (other == client.player || !greeted.add(other.getUUID())) continue;
            client.player.connection.sendCommand("msg " + other.getName().getString() + " " + message.get());
            return;
        }
    }
}
