package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.FishingHookAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

public final class AutoFish extends Module {
    private int cooldown;
    private int currentHookId = -1;
    private boolean reeled;

    public AutoFish() {
        super("auto_fish", "Auto Fish", "Cast, reel on a bite, and recast without packet spam.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        if (client.gui.screen() != null || client.gameMode == null) return;
        InteractionHand hand;
        if (client.player.getMainHandItem().getItem() == Items.FISHING_ROD) hand = InteractionHand.MAIN_HAND;
        else if (client.player.getOffhandItem().getItem() == Items.FISHING_ROD) hand = InteractionHand.OFF_HAND;
        else { currentHookId = -1; reeled = false; return; }

        FishingHook hook = client.player.fishing;
        if (hook == null) {
            currentHookId = -1;
            reeled = false;
            if (cooldown == 0) {
                client.gameMode.useItem(client.player, hand);
                cooldown = 20;
            }
            return;
        }
        if (hook.getId() != currentHookId) {
            currentHookId = hook.getId();
            reeled = false;
        }
        if (!reeled && cooldown == 0 && ((FishingHookAccessor) hook).anomoly$isBiting()) {
            client.gameMode.useItem(client.player, hand);
            reeled = true;
            cooldown = 20;
        }
    }

    @Override protected void onDisable(Minecraft client) {
        cooldown = 0;
        currentHookId = -1;
        reeled = false;
    }
}
