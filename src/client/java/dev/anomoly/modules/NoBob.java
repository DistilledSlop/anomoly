package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class NoBob extends Module {
    private Boolean previous;

    public NoBob() {
        super("no_bob", "No Bob", "Disable walking camera bob and restore your prior setting afterward.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        if (previous == null) previous = client.options.bobView().get();
        if (client.options.bobView().get()) client.options.bobView().set(false);
    }

    @Override protected void onDisable(Minecraft client) {
        if (previous != null) client.options.bobView().set(previous);
        previous = null;
    }
}
