package dev.anomoly.modules;

public final class HeadRoll extends SilentRotation {
    public HeadRoll() {
        super("head_roll", "HeadRoll", "Make your head nod up and down continuously for other players.");
    }

    @Override protected float[] angles(int step, float yaw, float pitch) {
        return new float[] {yaw, (float)Math.sin(step * 0.5) * 90F};
    }
}
