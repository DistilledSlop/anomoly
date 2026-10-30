package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;

public final class RangeCircle extends Module {
    private final FloatSetting radius = add(new FloatSetting("radius", "Radius", 3.0F, 1.0F, 10.0F));

    public RangeCircle() {
        super("range_circle", "RangeCircle", "Draw a circle of a chosen radius around you on the ground.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> Gizmos.circle(client.player.position().add(0, 0.05, 0), radius.get(),
            GizmoStyle.stroke(0xFF55DDBB, 2.0F)));
    }
}
