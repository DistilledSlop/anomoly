package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

public final class Breadcrumbs extends Module {
    private final FloatSetting length = add(new FloatSetting("length", "Trail points", 200, 20, 2000));
    private final Deque<Vec3> trail = new ArrayDeque<>();

    public Breadcrumbs() {
        super("breadcrumbs", "Breadcrumbs", "Draw a line along the path you have walked (also covers Trail and Trails).", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { trail.clear(); }

    @Override public void tick(Minecraft client) {
        Vec3 here = client.player.position().add(0, 0.1, 0);
        if (trail.isEmpty() || trail.peekLast().distanceToSqr(here) > 0.04) trail.addLast(here);
        while (trail.size() > length.get()) trail.removeFirst();
        WorldGizmos.draw(client, () -> {
            Vec3 previous = null;
            for (Vec3 point : trail) {
                if (previous != null) Gizmos.line(previous, point, 0xFF55DDBB, 2.0F);
                previous = point;
            }
        });
    }
}
