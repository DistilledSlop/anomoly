package dev.anomoly.modules;

public final class AntiAim extends SilentRotation {
    public AntiAim() {
        super("anti_aim", "Anti Aim", "Show the server a spinning yaw and flipping pitch while your real view stays put, so aimed shots misjudge you.", Category.MISC);
    }

    @Override protected float[] angles(int step, float yaw, float pitch) {
        return new float[] {step * 37F % 360F - 180F, step % 2 == 0 ? 90F : -90F};
    }
}
