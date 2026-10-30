package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

/** Repeats shots while the player holds use, honoring server charge durations. */
public final class BowSpam extends Module {
    private final FloatSetting bowCharge = add(new FloatSetting("bow_charge_ticks", "Bow charge ticks", 20F, 3F, 20F));

    public BowSpam() {
        super("bow_spam", "Bow Spam", "Repeatedly release a held bow or crossbow while use is held.", Category.COMBAT);
    }

    public boolean active(Minecraft client) {
        return enabled() && client.player != null && client.gameMode != null && client.gui.screen() == null
            && client.options.keyUse.isDown() && client.player.isUsingItem()
            && (client.player.getUseItem().getItem() instanceof BowItem
                || client.player.getUseItem().getItem() instanceof CrossbowItem);
    }

    @Override public void tick(Minecraft client) {
        if (!active(client) || AnomolyClient.MODULES.get(AutoBow.class).active(client)) return;
        ItemStack stack = client.player.getUseItem();
        int required = stack.getItem() instanceof BowItem ? (int)bowCharge.get()
            : CrossbowItem.getChargeDuration(stack, client.player) + 1;
        if (client.player.getTicksUsingItem() >= required)
            client.gameMode.releaseUsingItem(client.player);
    }
}
