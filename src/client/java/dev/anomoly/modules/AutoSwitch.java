package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AutoSwitch extends Module {
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Switch every ticks", 10, 2, 100));
    private final BooleanSetting skipEmpty = add(new BooleanSetting("skip_empty", "Skip empty slots", true));
    private final BooleanSetting restore = add(new BooleanSetting("restore", "Restore original slot", true));
    private int ticks;
    private int originalSlot = -1;
    private int lastAutoSlot = -1;
    private LocalPlayer observedPlayer;

    public AutoSwitch() {
        super("auto_switch", "Auto Switch", "Cycle held hotbar items on a timer.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (observedPlayer != client.player) {
            ticks = 0;
            originalSlot = -1;
            lastAutoSlot = -1;
            observedPlayer = client.player;
        }
        if (client.gui.screen() != null || client.player.isUsingItem() || client.options.keyAttack.isDown()
            || client.options.keyUse.isDown()
            || client.gameMode == null || client.gameMode.isDestroying()
            || AnomolyClient.MODULES.get(KillAura.class).enabled()) return;
        var inventory = client.player.getInventory();
        if (originalSlot < 0) originalSlot = inventory.getSelectedSlot();
        if (++ticks < (int)interval.get()) return;
        ticks = 0;
        int current = inventory.getSelectedSlot();
        for (int offset = 1; offset < 9; offset++) {
            int next = (current + offset) % 9;
            if (skipEmpty.get() && inventory.getItem(next).isEmpty()) continue;
            inventory.setSelectedSlot(next);
            lastAutoSlot = next;
            return;
        }
    }

    @Override protected void onDisable(Minecraft client) {
        if (restore.get() && originalSlot >= 0 && client.player != null
            && client.player.getInventory().getSelectedSlot() == lastAutoSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        ticks = 0;
        originalSlot = -1;
        lastAutoSlot = -1;
        observedPlayer = null;
    }
}
