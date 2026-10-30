package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class AntiStaff extends Module {
    private final StringSetting staff = add(new StringSetting("staff", "Staff names (comma separated)", ""));
    private final Set<UUID> alerted = new HashSet<>();

    public AntiStaff() {
        super("anti_staff", "AntiStaff", "Warn in chat when a player whose name you listed as staff is on the server.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { alerted.clear(); }

    @Override public void tick(Minecraft client) {
        Set<UUID> online = new HashSet<>();
        for (PlayerInfo info : client.player.connection.getOnlinePlayers()) {
            online.add(info.getProfile().id());
            String name = info.getProfile().name().toLowerCase(Locale.ROOT);
            for (String entry : staff.get().split(",")) {
                if (!entry.isBlank() && entry.trim().toLowerCase(Locale.ROOT).equals(name) && alerted.add(info.getProfile().id())) {
                    client.player.sendSystemMessage(Component.literal("[Anomoly] Staff member online: " + info.getProfile().name()));
                }
            }
        }
        alerted.retainAll(online);
    }
}
