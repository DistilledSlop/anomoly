package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;

public final class AutoSprint extends Module {
    public AutoSprint() {
        super("auto_sprint", "Auto Sprint", "Sprint while moving forward when vanilla permits it.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() == null && !AnomolyClient.MODULES.get(WTap.class).tapping(client)
            && client.player.input.hasForwardImpulse()
            && !client.player.isShiftKeyDown() && !client.player.isUsingItem()) {
            client.player.setSprinting(true);
        }
    }
}
