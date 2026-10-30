package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class Teams extends Module {
    private final StringSetting names = add(new StringSetting("names", "Friend names (comma separated)", ""));
    private final BooleanSetting scoreboard = add(new BooleanSetting("scoreboard", "Also protect scoreboard teammates", true));

    public Teams() {
        super("teams", "Teams", "Make Kill Aura, Multi Aura, Click Aura and the other target pickers skip listed friends and scoreboard teammates.", Category.MISC);
    }

    public boolean protects(Minecraft client, Player other) {
        if (!enabled() || other == client.player) return false;
        if (scoreboard.get() && client.player != null && client.player.isAlliedTo(other)) return true;
        return listed(other.getName().getString());
    }

    public boolean listed(String name) {
        for (String entry : names.get().split(",")) if (entry.trim().equalsIgnoreCase(name)) return true;
        return false;
    }

    public void toggle(String name) {
        StringBuilder out = new StringBuilder();
        boolean found = false;
        for (String entry : names.get().split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.equalsIgnoreCase(name)) { found = true; continue; }
            out.append(out.length() == 0 ? "" : ",").append(trimmed);
        }
        if (!found) out.append(out.length() == 0 ? "" : ",").append(name);
        names.set(out.toString());
    }
}
