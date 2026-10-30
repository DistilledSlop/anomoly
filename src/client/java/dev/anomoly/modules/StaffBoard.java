package dev.anomoly.modules;

import dev.anomoly.setting.StringSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class StaffBoard extends TextHud {
    private final StringSetting staff = add(new StringSetting("staff", "Staff names (comma separated)", ""));

    public StaffBoard() {
        super("staff_board", "StaffBoard", "List the names you marked as staff that are currently on the server, and mark any missing from the world.");
    }

    protected boolean isStaff(String name) {
        for (String entry : staff.get().split(",")) if (!entry.isBlank() && entry.trim().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    @Override protected List<String> lines(Minecraft client) {
        List<String> out = new ArrayList<>();
        for (PlayerInfo info : client.player.connection.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (isStaff(name)) out.add(name + (client.level.getPlayerByUUID(info.getProfile().id()) != null ? " (nearby)" : ""));
        }
        return out;
    }
}
