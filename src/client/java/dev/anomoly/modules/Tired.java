package dev.anomoly.modules;

public final class Tired extends SilentRotation {
    public Tired() {
        super("tired", "Tired", "Make your head droop toward your chest and sway slowly for other players.");
    }

    @Override protected float[] angles(int step, float yaw, float pitch) {
        return new float[] {yaw + (float)Math.sin(step * 0.05) * 20F, 75F + (float)Math.sin(step * 0.1) * 10F};
    }
}
