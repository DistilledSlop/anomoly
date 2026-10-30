package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

public final class AntiAFK extends Module {
    private final FloatSetting idleSeconds = add(new FloatSetting("idle_seconds", "Idle seconds", 60, 10, 300));
    private final FloatSetting actionSeconds = add(new FloatSetting("action_seconds", "Action seconds", 20, 5, 120));
    private final BooleanSetting rotate = add(new BooleanSetting("rotate", "Turn view", true));
    private final BooleanSetting swing = add(new BooleanSetting("swing", "Swing hand", true));
    private final BooleanSetting jump = add(new BooleanSetting("jump", "Jump", false));
    private int idleTicks;
    private int actionTicks;
    private boolean turnRight;
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;
    private boolean tracking;

    public AntiAFK() {
        super("anti_afk", "Anti AFK", "Act after being idle, without interrupting movement or menus.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null) { tracking = false; idleTicks = 0; actionTicks = 0; return; }
        var player = client.player;
        boolean moved = tracking && (Math.abs(player.getX() - lastX) > 0.01
            || Math.abs(player.getY() - lastY) > 0.01 || Math.abs(player.getZ() - lastZ) > 0.01
            || Math.abs(player.getYRot() - lastYaw) > 0.1 || Math.abs(player.getXRot() - lastPitch) > 0.1);
        boolean input = client.options.keyUp.isDown() || client.options.keyDown.isDown()
            || client.options.keyLeft.isDown() || client.options.keyRight.isDown()
            || client.options.keyJump.isDown() || client.options.keyShift.isDown();
        if (moved || input) { idleTicks = 0; actionTicks = 0; }
        else {
            idleTicks++;
            if (idleTicks >= (int)(idleSeconds.get() * 20) && ++actionTicks >= (int)(actionSeconds.get() * 20)) {
                actionTicks = 0;
                if (rotate.get()) {
                    player.setYRot(player.getYRot() + (turnRight ? 2.0F : -2.0F));
                    turnRight = !turnRight;
                }
                if (swing.get()) player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
                if (jump.get() && player.onGround()) player.jumpFromGround();
            }
        }
        lastX = player.getX(); lastY = player.getY(); lastZ = player.getZ();
        lastYaw = player.getYRot(); lastPitch = player.getXRot();
        tracking = true;
    }

    @Override protected void onDisable(Minecraft client) {
        tracking = false;
        idleTicks = 0;
        actionTicks = 0;
    }
}
