package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;

public final class LagReducer extends Module {
    private final IntSetting limit = add(new IntSetting("fps", "FPS limit while unfocused", 15, 5, 60));
    private Integer original;

    public LagReducer() {
        super("lag_reducer", "LagReducer", "Cap the frame rate while the game window is not focused (also covers UnfocusedFPS).", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        var option = client.options.framerateLimit();
        if (!client.isWindowActive()) {
            if (original == null) original = option.get();
            option.set(limit.get());
        } else restore(client);
    }

    private void restore(Minecraft client) {
        if (original != null) client.options.framerateLimit().set(original);
        original = null;
    }

    @Override protected void onDisable(Minecraft client) { restore(client); }
}
