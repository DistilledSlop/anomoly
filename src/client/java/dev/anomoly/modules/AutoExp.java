package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Throws experience bottles at the player's feet when worn Mending gear needs repair. */
public final class AutoExp extends Module {
    private final FloatSetting repairBelow = add(new FloatSetting("repair_below_percent", "Repair below durability %", 85F, 5F, 99F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Minimum ticks between bottles", 8F, 3F, 40F));
    private final BooleanSetting armor = add(new BooleanSetting("armor", "Repair armor", true));
    private final BooleanSetting hands = add(new BooleanSetting("hands", "Repair held tools", true));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select bottles in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private int cooldown;

    public AutoExp() {
        super("auto_exp", "Auto Exp", "Throw experience bottles at your feet when equipped Mending gear needs repair.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        if (client.gameMode == null || client.gui.screen() != null || client.player.isSpectator()
            || client.player.isUsingItem() || client.player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(ThrowPot.class).active(client)
            || AnomolyClient.MODULES.get(AutoPotion.class).active(client)
            || AnomolyClient.MODULES.get(Pot.class).active(client)) return;
        LocalPlayer player = client.player;
        InteractionHand hand = null;
        int originalSlot = -1;
        if (player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE) && needsRepair(player, InteractionHand.OFF_HAND))
            hand = InteractionHand.OFF_HAND;
        else if (player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE) && needsRepair(player, InteractionHand.MAIN_HAND))
            hand = InteractionHand.MAIN_HAND;
        else if (selectHotbar.get() && needsRepair(player, InteractionHand.MAIN_HAND)) {
            for (int slot = 0; slot < 9; slot++) {
                if (!player.getInventory().getItem(slot).is(Items.EXPERIENCE_BOTTLE)) continue;
                originalSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(slot);
                hand = InteractionHand.MAIN_HAND;
                break;
            }
        }
        if (hand == null) return;
        ItemStack bottle = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(bottle)) { restore(player, originalSlot); return; }
        float originalPitch = player.getXRot();
        player.setXRot(85F);
        InteractionResult result = client.gameMode.useItem(player, hand);
        player.setXRot(originalPitch);
        if (result instanceof InteractionResult.Success) {
            player.swing(hand, bottle.getInteractAnimation(), false);
            player.itemUsed(hand);
            cooldown = (int)interval.get();
        }
        restore(player, originalSlot);
    }

    private boolean needsRepair(LocalPlayer player, InteractionHand bottleHand) {
        if (armor.get()) for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) if (damagedMending(player.getItemBySlot(slot))) return true;
        if (!hands.get()) return false;
        ItemStack other = player.getItemInHand(bottleHand == InteractionHand.MAIN_HAND
            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return damagedMending(other);
    }

    private boolean damagedMending(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getDamageValue() <= 0) return false;
        float remaining = 100F * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
        if (remaining >= repairBelow.get()) return false;
        for (var entry : stack.getEnchantments().entrySet())
            if (entry.getIntValue() > 0 && entry.getKey().unwrapKey()
                .map(key -> key.identifier().getPath().equals("mending")).orElse(false)) return true;
        return false;
    }

    private void restore(LocalPlayer player, int slot) {
        if (restoreSlot.get() && slot >= 0 && player.getInventory().getSelectedSlot() != slot)
            player.getInventory().setSelectedSlot(slot);
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
