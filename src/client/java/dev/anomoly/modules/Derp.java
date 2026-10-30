package dev.anomoly.modules;

import java.util.concurrent.ThreadLocalRandom;

public final class Derp extends SilentRotation {
    public Derp() {
        super("derp", "Derp", "Show other players a head that looks around at random while your own view stays put.");
    }

    @Override protected float[] angles(int step, float yaw, float pitch) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new float[] {random.nextFloat() * 360F - 180F, random.nextFloat() * 180F - 90F};
    }
}
