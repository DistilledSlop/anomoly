package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Replace a grounded jump's launch velocity; optionally soften the descent. */
public final class HighJump extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Vanilla", "Vanilla", "Vulcan"));
    private final FloatSetting motion = add(new FloatSetting("motion", "Jump motion", 0.8F, 0.2F, 10.0F));
    private final BooleanSetting glide = add(new BooleanSetting("glide", "Vulcan glide", false));
    private boolean launched;
    private int launchTick;

    public HighJump() {
        super("high_jump", "HighJump", "Set jump launch strength; optional Vulcan descent mode.", Category.MOVEMENT);
    }

    public void onJump(LocalPlayer player) {
        if (!enabled() || player != Minecraft.getInstance().player || !player.onGround()) return;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, motion.get(), velocity.z);
        launched = true;
        launchTick = player.tickCount;
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!launched) return;
        if (player.onGround() && player.tickCount > launchTick) {
            launched = false;
            return;
        }
        if (!mode.get().equals("Vulcan") || !glide.get()) return;
        if (player.tickCount <= launchTick || player.fallDistance <= 0 || player.tickCount % 2 != 0) return;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y < -0.155) player.setDeltaMovement(velocity.x, -0.155, velocity.z);
    }

    @Override protected void onDisable(Minecraft client) {
        launched = false;
    }
}
