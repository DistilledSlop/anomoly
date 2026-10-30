package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class Waypoints extends Module {
    private final StringSetting points = add(new StringSetting("points", "Waypoints (name x y z; ...)", ""));
    private final BooleanSetting addHere = add(new BooleanSetting("add_here", "Add waypoint at current position", false));
    private final BooleanSetting lines = add(new BooleanSetting("lines", "Draw lines to waypoints", true));
    private int counter;

    public Waypoints() {
        super("waypoints", "Waypoints", "Save coordinates in the settings and draw labelled markers for them in the world (also covers Marker).", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (addHere.get()) {
            addHere.set(false);
            String entry = String.format(Locale.ROOT, "wp%d %.0f %.0f %.0f", ++counter, player.getX(), player.getY(), player.getZ());
            points.set(points.get().isBlank() ? entry : points.get() + "; " + entry);
            player.sendSystemMessage(Component.literal("[Anomoly] Saved waypoint " + entry));
        }
        WorldGizmos.draw(client, () -> {
            for (String raw : points.get().split(";")) {
                String[] parts = raw.trim().split("\\s+");
                if (parts.length != 4) continue;
                try {
                    Vec3 at = new Vec3(Double.parseDouble(parts[1]) + 0.5, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]) + 0.5);
                    Gizmos.billboardText(parts[0] + " " + (int)player.position().distanceTo(at) + "m", at.add(0, 1, 0),
                        net.minecraft.gizmos.TextGizmo.Style.forColorAndCentered(0xFFFFFF55)).setAlwaysOnTop();
                    Gizmos.point(at, 0xFFFFFF55, 8.0F).setAlwaysOnTop();
                    if (lines.get()) Gizmos.line(player.getEyePosition(), at, 0x80FFFF55, 1.0F).setAlwaysOnTop();
                } catch (NumberFormatException ignored) {
                    // Skip malformed entries so one typo does not hide the rest.
                }
            }
        });
    }
}
