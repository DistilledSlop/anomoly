package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class FreeCam extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Speed (blocks/tick)", 0.8F, 0.1F, 4.0F));
    private Vec3 position = Vec3.ZERO;
    private float yaw, pitch;
    private CameraType previousPerspective;

    public FreeCam() {
        super("free_cam", "FreeCam", "Detach the camera from your body and fly it around while the player stands still.", Category.RENDER);
    }

    public boolean active() {
        Minecraft client = Minecraft.getInstance();
        return enabled() && client.player != null && client.level != null;
    }

    public Vec3 position() { return position; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    public void turn(double horizontal, double vertical) {
        yaw = Mth.wrapDegrees(yaw + (float)horizontal * 0.15F);
        pitch = Mth.clamp(pitch + (float)vertical * 0.15F, -90.0F, 90.0F);
    }

    /** Stops the body from walking while the camera is the thing being steered. */
    public void freezeBody(KeyboardInput input) {
        if (!active()) return;
        input.keyPresses = new Input(false, false, false, false, false, false, false);
        ((dev.anomoly.mixin.ClientInputAccessor)input).anomoly$setMoveVector(Vec2.ZERO);
    }

    @Override protected void onEnable(Minecraft client) {
        if (client.player == null) return;
        position = client.player.getEyePosition();
        yaw = client.player.getYRot();
        pitch = client.player.getXRot();
        previousPerspective = client.options.getCameraType();
        if (previousPerspective.isFirstPerson()) client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @Override protected void onDisable(Minecraft client) {
        if (previousPerspective != null) client.options.setCameraType(previousPerspective);
        previousPerspective = null;
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null) return;
        var o = client.options;
        double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double strafe = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        double vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        double radians = Math.toRadians(yaw);
        double dx = -Math.sin(radians) * forward + Math.cos(radians) * strafe;
        double dz = Math.cos(radians) * forward + Math.sin(radians) * strafe;
        position = position.add(dx * speed.get(), vertical * speed.get(), dz * speed.get());
    }

    public static FreeCam get() { return AnomolyClient.MODULES.get(FreeCam.class); }
}
