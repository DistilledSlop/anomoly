package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ElytraFly extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Control", "Control", "Boost"));
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed", 1.0F, 0.1F, 4.0F));
    private final FloatSetting vertical = add(new FloatSetting("vertical", "Vertical speed", 0.5F, 0.0F, 2.0F));

    public ElytraFly() {
        super("elytra_fly", "ElytraFly", "Steer elytra flight directly: forward flies where you look, jump climbs, sneak descends (also covers Elytra Boost).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.isFallFlying()) return;
        Vec3 look = player.getLookAngle();
        Vec3 velocity = player.getDeltaMovement();
        if (mode.get().equals("Boost")) {
            if (client.options.keyUp.isDown() && velocity.length() < speed.get() * 2) {
                player.setDeltaMovement(velocity.add(look.scale(speed.get() * 0.05)));
            }
            return;
        }
        double y = look.y * (client.options.keyUp.isDown() ? speed.get() : 0);
        if (client.options.keyJump.isDown()) y = vertical.get();
        else if (client.options.keyShift.isDown()) y = -vertical.get();
        else if (!client.options.keyUp.isDown()) y = 0;
        Vec3 flat = client.options.keyUp.isDown() ? new Vec3(look.x, 0, look.z).normalize().scale(speed.get()) : Vec3.ZERO;
        player.setDeltaMovement(flat.x, y, flat.z);
    }
}
