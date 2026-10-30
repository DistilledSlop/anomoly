package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import net.minecraft.network.chat.Component;

public final class ChatAppend extends Module implements ChatFilter {
    private final StringSetting suffix = add(new StringSetting("suffix", "Suffix", " | Anomoly"));

    public ChatAppend() {
        super("chat_append", "ChatAppend", "Add a fixed suffix to every chat message you send (also covers MessageAppend).", Category.MISC);
    }

    @Override public Component onIncoming(Component message) { return message; }

    @Override public String onOutgoing(String message) {
        String result = message + suffix.get();
        return message.startsWith("/") || result.length() > 256 ? message : result;
    }
}
