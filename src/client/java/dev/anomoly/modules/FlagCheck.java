package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class FlagCheck extends Module {
    private int flags;

    public FlagCheck() {
        super("flag_check", "FlagCheck", "Count and announce each time the server teleports you back, a sign it rejected your movement.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { flags = 0; }

    public void flagged(Minecraft client) {
        if (!enabled() || client.player == null) return;
        client.player.sendSystemMessage(Component.literal("[Anomoly] Server position correction #" + ++flags));
    }
}
