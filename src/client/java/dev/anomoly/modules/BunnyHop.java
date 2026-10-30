package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class BunnyHop extends Module {
    private final FloatSetting boost = add(new FloatSetting("boost", "Jump boost", 0.12F, 0.0F, 0.6F));

    public BunnyHop() {
        super("bunny_hop", "BunnyHop", "Hop continuously while moving and add forward speed on each takeoff.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.onGround() || !MoveUtil.moving(player) || player.isShiftKeyDown()
            || !MoveUtil.freeMovement(player) || player.isInWater() || player.isInLava()) return;
        player.jumpFromGround();
        Vec3 push = MoveUtil.direction(player, boost.get());
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x + push.x, velocity.y, velocity.z + push.z);
    }
}
