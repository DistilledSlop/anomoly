package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Skeleton extends Module {
    public Skeleton() {
        super("skeleton", "Skeleton", "Draw a stick-figure skeleton through walls over other players.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        WorldGizmos.draw(client, () -> {
            for (Player other : client.level.players()) {
                if (other == client.player || client.player.distanceToSqr(other) > 64 * 64) continue;
                double yaw = Math.toRadians(other.yBodyRot);
                Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(0.3);
                Vec3 base = other.position();
                double h = other.getBbHeight();
                Vec3 head = base.add(0, h * 0.92, 0), neck = base.add(0, h * 0.82, 0), hip = base.add(0, h * 0.45, 0);
                int c = 0xFFFFFFFF;
                Gizmos.line(head, neck, c, 2.0F).setAlwaysOnTop();
                Gizmos.line(neck, hip, c, 2.0F).setAlwaysOnTop();
                Gizmos.line(neck.add(side), neck.subtract(side), c, 2.0F).setAlwaysOnTop();
                Gizmos.line(neck.add(side), base.add(0, h * 0.5, 0).add(side.scale(1.2)), c, 2.0F).setAlwaysOnTop();
                Gizmos.line(neck.subtract(side), base.add(0, h * 0.5, 0).subtract(side.scale(1.2)), c, 2.0F).setAlwaysOnTop();
                Gizmos.line(hip, base.add(side.scale(0.5)), c, 2.0F).setAlwaysOnTop();
                Gizmos.line(hip, base.subtract(side.scale(0.5)), c, 2.0F).setAlwaysOnTop();
            }
        });
    }
}
