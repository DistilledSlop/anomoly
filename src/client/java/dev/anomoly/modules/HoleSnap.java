package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class HoleSnap extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Search range", 4.0F, 1.0F, 8.0F));
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed", 0.25F, 0.05F, 0.8F));

    public HoleSnap() {
        super("hole_snap", "HoleSnap", "Walk to the nearest crystal-safe hole and settle in its center (also covers AutoHole and HoleAnchor).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !MoveUtil.freeMovement(player)) return;
        BlockPos hole = Holes.nearest(client.level, player.blockPosition(), (int)range.get(), 3);
        if (hole == null) return;
        Vec3 center = Vec3.atBottomCenterOf(hole);
        double dx = center.x - player.getX();
        double dz = center.z - player.getZ();
        double distance = Math.hypot(dx, dz);
        Vec3 velocity = player.getDeltaMovement();
        if (distance < 0.1) {
            player.setDeltaMovement(0, velocity.y, 0);
            player.setPos(center.x, player.getY(), center.z);
            return;
        }
        double step = Math.min(distance, speed.get());
        player.setDeltaMovement(dx / distance * step, velocity.y, dz / distance * step);
    }
}
