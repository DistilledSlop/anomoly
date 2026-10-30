package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class LongJump extends Module {
    private final FloatSetting boost = add(new FloatSetting("boost", "Horizontal boost", 0.5F, 0.1F, 2.0F));
    private final FloatSetting lift = add(new FloatSetting("lift", "Extra lift", 0.05F, 0.0F, 0.5F));
    private boolean wasGrounded = true;

    public LongJump() {
        super("long_jump", "LongJump", "Add forward momentum and lift to every jump to cover more ground.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean takeoff = wasGrounded && !player.onGround() && player.getDeltaMovement().y > 0.1;
        wasGrounded = player.onGround();
        if (!takeoff || !MoveUtil.moving(player) || !MoveUtil.freeMovement(player) || player.isInWater()) return;
        Vec3 push = MoveUtil.direction(player, boost.get());
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x + push.x, velocity.y + lift.get(), velocity.z + push.z);
    }
}
