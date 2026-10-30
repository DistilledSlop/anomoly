package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;

/** Automatically throws a safe instant-health splash potion at low health. */
public final class AutoPotion extends Module {
    private final FloatSetting health = add(new FloatSetting("health", "Health threshold", 12F, 1F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Minimum ticks between potions", 12F, 5F, 60F));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select potion in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private int cooldown;

    public AutoPotion() {
        super("auto_potion", "Auto Potion", "Automatically throw a healing splash potion at your feet when health is low.", Category.COMBAT);
    }

    public boolean active(Minecraft client) {
        return enabled() && client.player != null && client.gameMode != null && client.gui.screen() == null
            && client.player.isAlive() && !client.player.isSpectator()
            && client.player.getHealth() <= health.get() && client.player.getHealth() < client.player.getMaxHealth()
            && HealingPotions.available(client.player, selectHotbar.get());
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        if (!active(client) || SoupUse.busy(client)
            || client.player.isUsingItem() || client.player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || AnomolyClient.MODULES.get(ThrowPot.class).active(client)
            || AnomolyClient.MODULES.get(Pot.class).active(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)) return;
        HealingPotions.Selection selection = HealingPotions.select(client.player, selectHotbar.get());
        if (HealingPotions.throwAtFeet(client, selection)) cooldown = (int)interval.get();
        HealingPotions.restore(client.player, selection, restoreSlot.get());
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
