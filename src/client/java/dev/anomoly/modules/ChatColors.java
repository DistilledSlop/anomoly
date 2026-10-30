package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.chat.Component;

public final class ChatColors extends Module implements ChatFilter {
    public ChatColors() {
        super("chat_colors", "ChatColors", "Turn &-codes in the chat you send into section-sign color codes.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) { return message; }

    @Override public String onOutgoing(String message) {
        return message.startsWith("/") ? message : message.replaceAll("&([0-9a-fk-or])", "§$1");
    }
}
