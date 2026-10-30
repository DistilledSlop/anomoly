package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.AnomolyClient;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BowItem;

public final class AutoBowRelease extends Module {
    private final FloatSetting chargeTicks = add(new FloatSetting("charge_ticks", "Release after ticks", 20, 3, 40));

    public AutoBowRelease() {
        super("auto_bow_release", "Auto Bow Release", "Release a drawn bow after the selected charge time.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || client.gui.screen() != null || client.gameMode == null || !client.player.isUsingItem()
            || !(client.player.getUseItem().getItem() instanceof BowItem)) return;
        if (client.player.getTicksUsingItem() >= (int)chargeTicks.get()) {
            client.gameMode.releaseUsingItem(client.player);
        }
    }
}
