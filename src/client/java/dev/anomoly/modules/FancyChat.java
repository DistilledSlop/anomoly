package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.chat.Component;

public final class FancyChat extends Module implements ChatFilter {
    private static final String NORMAL = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";

    public FancyChat() {
        super("fancy_chat", "Fancy Chat", "Send your chat messages in small-caps letters.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) { return message; }

    @Override public String onOutgoing(String message) {
        if (message.startsWith("/")) return message;
        StringBuilder out = new StringBuilder();
        for (char c : message.toCharArray()) {
            int index = NORMAL.indexOf(Character.toLowerCase(c));
            out.append(index >= 0 && Character.isLowerCase(c) ? SMALL.charAt(index) : c);
        }
        return out.toString();
    }
}
