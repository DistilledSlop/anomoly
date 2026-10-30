package dev.anomoly.modules;

import dev.anomoly.Module;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class Timestamps extends Module implements ChatFilter {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public Timestamps() {
        super("timestamps", "Timestamps", "Prefix each chat line with the local time it arrived.", Category.MISC);
    }

    @Override public Component onIncoming(Component message) {
        return Component.literal("<" + LocalTime.now().format(FORMAT) + "> ").withStyle(ChatFormatting.GRAY).append(message);
    }
}
