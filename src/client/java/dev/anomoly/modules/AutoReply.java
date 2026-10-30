package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AutoReply extends Module implements ChatFilter {
    private final StringSetting marker = add(new StringSetting("marker", "Whisper text contains", "whispers to you"));
    private final StringSetting reply = add(new StringSetting("reply", "Reply", "I'm away right now."));
    private long lastReply;

    public AutoReply() {
        super("auto_reply", "AutoReply", "Answer private messages with a canned reply, at most once every ten seconds.", Category.PLAYER);
    }

    @Override public Component onIncoming(Component message) {
        String text = message.getString();
        long now = System.currentTimeMillis();
        int split = text.toLowerCase(Locale.ROOT).indexOf(marker.get().toLowerCase(Locale.ROOT));
        if (split <= 0 || now - lastReply < 10_000) return message;
        String sender = text.substring(0, split).replaceAll("[^A-Za-z0-9_]", "").trim();
        if (sender.isEmpty()) return message;
        lastReply = now;
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> { if (client.player != null) client.player.connection.sendCommand("msg " + sender + " " + reply.get()); });
        return message;
    }
}
