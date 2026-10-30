package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

/** Uses soup when health is low, including on kit PvP servers with instant soup healing. */
public final class AutoSoup extends Module {
    private final FloatSetting health = add(new FloatSetting("health", "Health threshold", 12F, 1F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between attempts", 20F, 5F, 100F));
    private final FloatSetting failedRetry = add(new FloatSetting("failed_retry_ticks", "Retry after failed use", 200F, 40F, 1200F));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select soup in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting otherStews = add(new BooleanSetting("other_stews", "Use rabbit and beetroot stew", false));
    private final SoupUse use = new SoupUse();
    private int cooldown;

    public AutoSoup() {
        super("auto_soup", "Auto Soup", "Use mushroom stew when health is low, including on soup PvP servers.", Category.COMBAT);
    }

    public boolean using(Minecraft client) { return enabled() && use.using(client); }

    @Override public void tick(Minecraft client) {
        if (use.active()) {
            if (use.tick(client)) cooldown = (int)(use.consumed() ? interval.get() : failedRetry.get());
            return;
        }
        if (cooldown > 0) { cooldown--; return; }
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isUsingItem() || client.player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || client.player.getHealth() > health.get()
            || AnomolyClient.MODULES.get(AutoPearl.class).active(client)
            || AnomolyClient.MODULES.get(Soup.class).using(client)) return;
        use.start(client, selectHotbar.get(), otherStews.get(), restoreSlot.get());
    }

    public void finishTick(Minecraft client) { use.finishTick(client); }

    @Override protected void onDisable(Minecraft client) {
        use.cancel(client);
        cooldown = 0;
    }
}
