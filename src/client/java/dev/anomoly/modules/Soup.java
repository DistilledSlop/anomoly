package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

/** One-shot soup use from a hotbar slot or either hand. */
public final class Soup extends Module {
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting otherStews = add(new BooleanSetting("other_stews", "Use rabbit and beetroot stew", false));
    private final SoupUse use = new SoupUse();

    public Soup() {
        super("soup", "Soup", "On toggle, select and use one mushroom stew.", Category.COMBAT);
    }

    public boolean using(Minecraft client) { return enabled() && use.using(client); }

    @Override public void tick(Minecraft client) {
        if (use.active()) {
            if (use.tick(client)) setEnabled(false);
            return;
        }
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isUsingItem() || client.player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || AnomolyClient.MODULES.get(AutoPearl.class).active(client)
            || AnomolyClient.MODULES.get(AutoSoup.class).using(client)) return;
        if (!use.start(client, true, otherStews.get(), restoreSlot.get())) setEnabled(false);
    }

    public void finishTick(Minecraft client) { use.finishTick(client); }

    @Override protected void onDisable(Minecraft client) { use.cancel(client); }
}
