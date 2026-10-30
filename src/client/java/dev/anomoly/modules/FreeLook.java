package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class FreeLook extends Module {
    private final BooleanSetting thirdPerson = add(new BooleanSetting("third_person", "Switch to third person", true));
    private boolean active;
    private boolean changedPerspective;
    private CameraType previousPerspective;
    private float yaw;
    private float pitch;

    public FreeLook() {
        super("free_look", "Free Look", "Hold H to look around without turning your player or changing movement direction.", Category.RENDER);
    }

    public boolean active(Minecraft client) {
        boolean shouldBeActive = enabled() && AnomolyClient.freeLookHeld()
            && client.player != null && client.level != null && client.gui.screen() == null;
        if (!shouldBeActive) {
            reset(client);
            return false;
        }
        if (!active) {
            active = true;
            yaw = client.player.getYRot();
            pitch = client.player.getXRot();
            previousPerspective = client.options.getCameraType();
            changedPerspective = thirdPerson.get() && previousPerspective.isFirstPerson();
            if (changedPerspective) client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        return true;
    }

    public void turn(double horizontal, double vertical) {
        yaw = Mth.wrapDegrees(yaw + (float)horizontal * 0.15F);
        pitch = Mth.clamp(pitch + (float)vertical * 0.15F, -90.0F, 90.0F);
    }

    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    @Override public void tick(Minecraft client) { active(client); }

    public void reset(Minecraft client) {
        if (!active) return;
        if (changedPerspective && client.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            client.options.setCameraType(previousPerspective);
        }
        active = false;
        changedPerspective = false;
        previousPerspective = null;
    }

    @Override protected void onDisable(Minecraft client) { reset(client); }
}
