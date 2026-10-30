package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoGApple extends Module {
    private final FloatSetting health = add(new FloatSetting("health", "Health threshold", 8, 1, 20));
    private final BooleanSetting enchanted = add(new BooleanSetting("enchanted", "Use enchanted apples", false));
    private final FloatSetting cooldownTicks = add(new FloatSetting("cooldown_ticks", "Cooldown ticks", 400, 40, 1200));
    private int originalSlot = -1;
    private int appleSlot = -1;
    private int consumingTicks;
    private int cooldown;

    public AutoGApple() {
        super("auto_gapple", "Auto GApple", "Eat a golden apple from the hotbar when health is low.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) cooldown--;
        if (originalSlot >= 0) {
            if (client.gui.screen() != null || client.player.getInventory().getSelectedSlot() != appleSlot
                || !isApple(client.player.getMainHandItem()) || client.player.hasEffect(MobEffects.ABSORPTION)
                || ++consumingTicks > 45) {
                finish(client);
                cooldown = (int)cooldownTicks.get();
            } else client.options.keyUse.setDown(true);
            return;
        }
        if (cooldown > 0 || client.gui.screen() != null || client.player.isUsingItem()
            || client.player.hasEffect(MobEffects.ABSORPTION) || client.player.getHealth() > health.get()) return;
        var inventory = client.player.getInventory();
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).is(Items.GOLDEN_APPLE)) { slot = i; break; }
            if (slot < 0 && enchanted.get() && inventory.getItem(i).is(Items.ENCHANTED_GOLDEN_APPLE)) slot = i;
        }
        if (slot < 0) return;
        originalSlot = inventory.getSelectedSlot();
        appleSlot = slot;
        consumingTicks = 0;
        inventory.setSelectedSlot(slot);
        client.options.keyUse.setDown(true);
    }

    private boolean isApple(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || enchanted.get() && stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    private void finish(Minecraft client) {
        KeyMapping.setAll();
        if (client.player != null && client.player.getInventory().getSelectedSlot() == appleSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        appleSlot = -1;
        consumingTicks = 0;
    }

    @Override protected void onDisable(Minecraft client) {
        if (originalSlot >= 0) finish(client);
        cooldown = 0;
    }
}
