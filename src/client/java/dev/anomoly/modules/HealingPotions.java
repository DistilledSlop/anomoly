package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** Shared effect validation and normal-item use for self-healing splash potions. */
final class HealingPotions {
    record Selection(InteractionHand hand, int previousSlot) { }

    private static long lastThrowTick = Long.MIN_VALUE;
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private HealingPotions() { }

    static boolean isHealing(ItemStack stack) {
        if (!stack.is(Items.SPLASH_POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        boolean heal = false;
        for (var effect : contents.getAllEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return false;
            if (effect.getEffect().equals(MobEffects.INSTANT_HEALTH)) heal = true;
        }
        return heal;
    }

    static boolean available(LocalPlayer player, boolean selectHotbar) {
        if (isHealing(player.getOffhandItem()) || isHealing(player.getMainHandItem())) return true;
        if (selectHotbar) for (int slot = 0; slot < 9; slot++)
            if (isHealing(player.getInventory().getItem(slot))) return true;
        return false;
    }

    static Selection select(LocalPlayer player, boolean selectHotbar) {
        if (isHealing(player.getOffhandItem())) return new Selection(InteractionHand.OFF_HAND, -1);
        if (isHealing(player.getMainHandItem())) return new Selection(InteractionHand.MAIN_HAND, -1);
        if (selectHotbar) for (int slot = 0; slot < 9; slot++) {
            if (!isHealing(player.getInventory().getItem(slot))) continue;
            int previous = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            return new Selection(InteractionHand.MAIN_HAND, previous);
        }
        return null;
    }

    static void restore(LocalPlayer player, Selection selection, boolean enabled) {
        if (enabled && selection != null && selection.previousSlot() >= 0)
            player.getInventory().setSelectedSlot(selection.previousSlot());
    }

    static boolean throwAtFeet(Minecraft client, Selection selection) {
        if (selection == null || client.gameMode == null || client.level == null) return false;
        if (lastLevel != client.level) {
            lastLevel = client.level;
            lastThrowTick = Long.MIN_VALUE;
        }
        long now = client.level.getGameTime();
        if (lastThrowTick != Long.MIN_VALUE && now - lastThrowTick < 5) return false;
        LocalPlayer player = client.player;
        ItemStack stack = player.getItemInHand(selection.hand());
        if (!isHealing(stack) || player.getCooldowns().isOnCooldown(stack)) return false;
        float pitch = player.getXRot();
        player.setXRot(85F);
        InteractionResult result = client.gameMode.useItem(player, selection.hand());
        player.setXRot(pitch);
        if (!(result instanceof InteractionResult.Success)) return false;
        player.swing(selection.hand(), stack.getInteractAnimation(), false);
        player.itemUsed(selection.hand());
        lastThrowTick = now;
        return true;
    }
}
