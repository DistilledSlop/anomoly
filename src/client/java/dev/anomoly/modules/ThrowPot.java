package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

/** One-shot toggle that throws one or two healing potions depending on health. */
public final class ThrowPot extends Module {
    private final FloatSetting twoBelow = add(new FloatSetting("two_below_health", "Throw two below health", 8F, 1F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between potions", 10F, 5F, 40F));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private int remaining = -1;
    private int cooldown;

    public ThrowPot() {
        super("throw_pot", "Throw Pot", "On toggle, throw one or two healing splash potions at your feet.", Category.COMBAT);
    }

    public boolean active(Minecraft client) { return enabled() && client.player != null && client.player.isAlive(); }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || client.player.isSpectator()) return;
        if (remaining < 0) remaining = client.player.getHealth() <= twoBelow.get() ? 2 : 1;
        if (client.player.getHealth() >= client.player.getMaxHealth() || remaining == 0
            || !HealingPotions.available(client.player, true)) { setEnabled(false); return; }
        if (cooldown > 0) { cooldown--; return; }
        if (client.player.isUsingItem() || client.player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)) return;
        HealingPotions.Selection selection = HealingPotions.select(client.player, true);
        if (HealingPotions.throwAtFeet(client, selection)) {
            remaining--;
            cooldown = (int)interval.get();
        }
        HealingPotions.restore(client.player, selection, restoreSlot.get());
    }

    @Override protected void onEnable(Minecraft client) { remaining = -1; cooldown = 0; }
    @Override protected void onDisable(Minecraft client) { remaining = -1; cooldown = 0; }
}
