package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class Fly extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Horizontal speed", 0.6F, 0.05F, 3.0F));
    private final FloatSetting vertical = add(new FloatSetting("vertical", "Vertical speed", 0.4F, 0.05F, 2.0F));
    private final BooleanSetting antiKick = add(new BooleanSetting("anti_kick", "Anti kick drop", true));
    private int ticks;

    public Fly() {
        super("fly", "Fly", "Hover and steer freely in survival with jump and sneak controlling height.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !MoveUtil.freeMovement(player)) return;
        double y = 0;
        if (client.options.keyJump.isDown()) y += vertical.get();
        if (client.options.keyShift.isDown()) y -= vertical.get();
        // Servers kick for floating; a tiny periodic drop resets the airtime check.
        if (antiKick.get() && y == 0 && ++ticks % 40 == 0) y = -0.04;
        Vec3 horizontal = MoveUtil.direction(player, speed.get());
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
        player.fallDistance = 0;
    }
}
