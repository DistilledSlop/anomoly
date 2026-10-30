package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class EntitySpeed extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed", 1.0F, 0.1F, 4.0F));

    public EntitySpeed() {
        super("entity_speed", "EntitySpeed", "Drive the vehicle or mount you control at a set speed (also covers VehicleBoost).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        Entity vehicle = player.getControlledVehicle();
        if (vehicle == null || !MoveUtil.moving(player)) return;
        Vec3 heading = MoveUtil.direction(player, speed.get());
        vehicle.setDeltaMovement(heading.x, vehicle.getDeltaMovement().y, heading.z);
    }
}
