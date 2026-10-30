package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TargetStrafe extends Module {
    private final FloatSetting radius = add(new FloatSetting("radius", "Orbit radius", 2.5F, 1.5F, 5.0F));
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed", 0.25F, 0.1F, 0.6F));
    private boolean clockwise = true;

    public TargetStrafe() {
        super("target_strafe", "TargetStrafe", "Circle around the nearest player while attack is held, keeping a fixed distance.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !client.options.keyAttack.isDown() || !MoveUtil.freeMovement(player)) return;
        Player target = null;
        for (Player other : client.level.players()) {
            if (other != player && player.distanceToSqr(other) < (radius.get() + 3) * (radius.get() + 3)
                && (target == null || player.distanceToSqr(other) < player.distanceToSqr(target))) target = other;
        }
        if (target == null) return;
        if (player.horizontalCollision) clockwise = !clockwise;
        Vec3 offset = new Vec3(player.getX() - target.getX(), 0, player.getZ() - target.getZ());
        double distance = Math.max(offset.length(), 1.0E-3);
        Vec3 radial = offset.scale(1 / distance);
        Vec3 tangent = new Vec3(-radial.z, 0, radial.x).scale(clockwise ? 1 : -1);
        Vec3 pull = radial.scale((radius.get() - distance) * 0.5);
        Vec3 heading = tangent.add(pull).normalize().scale(speed.get());
        MoveUtil.setHorizontal(player, heading);
    }
}
