package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Trajectories extends Module {
    public Trajectories() {
        super("trajectories", "Trajectories", "Draw the predicted flight path of the arrow, trident, pearl or throwable you are holding.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        ProjectilePhysics.Profile profile = ProjectilePhysics.held(client, false);
        if (profile == null) return;
        Vec3 position = player.getEyePosition().subtract(0, profile.spawnBelowEye(), 0);
        Vec3 velocity = Entity.calculateViewVector(player.getXRot() + profile.aimPitchOffset(), player.getYRot()).scale(profile.speed());
        if (profile.inheritsShooterMotion()) {
            Vec3 own = player.getDeltaMovement();
            velocity = velocity.add(own.x, player.onGround() ? 0 : own.y, own.z);
        }
        Vec3 finalVelocity = velocity;
        WorldGizmos.draw(client, () -> {
            Vec3 pos = position;
            Vec3 v = finalVelocity;
            for (int tick = 0; tick < 150; tick++) {
                if (profile.gravityBeforeMove()) v = v.subtract(0, profile.gravity(), 0);
                Vec3 next = pos.add(v);
                HitResult hit = client.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                boolean landed = hit.getType() != HitResult.Type.MISS;
                Gizmos.line(pos, landed ? hit.getLocation() : next, 0xFFFFFF55, 2.0F);
                if (landed) {
                    Gizmos.cuboid(new net.minecraft.world.phys.AABB(hit.getLocation().subtract(0.15, 0.15, 0.15), hit.getLocation().add(0.15, 0.15, 0.15)),
                        GizmoStyle.stroke(0xFFFF5555, 2.0F));
                    break;
                }
                pos = next;
                v = v.scale(profile.drag());
                if (!profile.gravityBeforeMove()) v = v.subtract(0, profile.gravity(), 0);
            }
        });
    }
}
