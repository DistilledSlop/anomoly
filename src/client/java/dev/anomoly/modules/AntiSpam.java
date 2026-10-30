package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.chat.Component;

public final class AntiSpam extends Module implements ChatFilter {
    private String last = "";

    public AntiSpam() {
        super("anti_spam", "AntiSpam", "Hide chat lines that repeat the previous line.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) {
        String text = message.getString();
        if (text.equals(last)) return null;
        last = text;
        return message;
    }
}
