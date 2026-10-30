package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.chat.Component;

public final class GreenText extends Module implements ChatFilter {
    public GreenText() {
        super("green_text", "GreenText", "Prefix every chat message you send with > for green text on servers that support it.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) { return message; }

    @Override public String onOutgoing(String message) {
        return message.startsWith("/") || message.startsWith(">") ? message : "> " + message;
    }
}
