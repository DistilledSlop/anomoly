package dev.anomoly.modules;

import dev.anomoly.Module;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class SoundEsp extends Module {
    private record Heard(Vec3 at, String name, long time) { }

    private final Deque<Heard> heard = new ArrayDeque<>();

    public SoundEsp() {
        super("sound_esp", "SoundESP", "Mark where sounds came from with a labelled point for a couple of seconds.", Category.RENDER);
    }

    public void heard(Identifier id, double x, double y, double z) {
        if (!enabled()) return;
        heard.addLast(new Heard(new Vec3(x, y, z), id.getPath(), System.currentTimeMillis()));
        while (heard.size() > 60) heard.removeFirst();
    }

    @Override public void tick(Minecraft client) {
        long now = System.currentTimeMillis();
        heard.removeIf(h -> now - h.time() > 2500);
        WorldGizmos.draw(client, () -> {
            for (Heard h : heard) {
                Gizmos.point(h.at(), 0xFFFFAA33, 8.0F).setAlwaysOnTop();
                Gizmos.billboardText(h.name(), h.at().add(0, 0.4, 0), net.minecraft.gizmos.TextGizmo.Style.forColorAndCentered(0xFFFFAA33)).setAlwaysOnTop();
            }
        });
    }
}
