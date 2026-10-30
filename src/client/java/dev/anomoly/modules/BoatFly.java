package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class BoatFly extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Horizontal speed", 1.0F, 0.1F, 4.0F));
    private final FloatSetting vertical = add(new FloatSetting("vertical", "Vertical speed", 0.5F, 0.1F, 2.0F));

    public BoatFly() {
        super("boat_fly", "BoatFly", "Fly a boat or other vehicle: jump rises, sneak sinks, otherwise it hovers.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        Entity vehicle = player.getControlledVehicle();
        if (vehicle == null || client.gui.screen() != null) return;
        double y = client.options.keyJump.isDown() ? vertical.get() : client.options.keyShift.isDown() ? -vertical.get() : 0;
        Vec3 heading = MoveUtil.direction(player, speed.get());
        vehicle.setDeltaMovement(heading.x, y, heading.z);
        vehicle.setNoGravity(true);
    }

    @Override protected void onDisable(Minecraft client) {
        Entity vehicle = client.player == null ? null : client.player.getControlledVehicle();
        if (vehicle != null) vehicle.setNoGravity(false);
    }
}
