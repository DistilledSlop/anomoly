package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.Locale;
import net.minecraft.network.protocol.Packet;

public final class PacketCanceller extends Module implements PacketInterceptor {
    private final StringSetting names = add(new StringSetting("packets", "Outgoing packet class names (comma separated)", ""));

    public PacketCanceller() {
        super("packet_canceller", "Packet Canceller", "Drop outgoing packets whose class name matches a configured list (also covers AntiPacket).", Category.MISC);
    }

    @Override public boolean intercept(Packet<?> packet) {
        String listed = names.get();
        if (listed.isBlank()) return false;
        String simple = packet.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        for (String name : listed.split(",")) if (!name.isBlank() && simple.equals(name.trim().toLowerCase(Locale.ROOT))) return true;
        return false;
    }
}
