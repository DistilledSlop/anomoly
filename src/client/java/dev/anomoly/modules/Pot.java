package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;

/** One-shot hotbar swap and throw at the player's current view direction. */
public final class Pot extends Module {
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));

    public Pot() {
        super("pot", "Pot", "On toggle, select and throw a splash or lingering potion from the hotbar.", Category.COMBAT);
    }

    public boolean active(Minecraft client) { return enabled() && client.player != null && client.player.isAlive(); }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || client.player.isSpectator()
            || client.player.isUsingItem() || client.player.isHandsBusy() || client.options.keyUse.isDown()) return;
        var inventory = client.player.getInventory();
        int originalSlot = inventory.getSelectedSlot();
        int potionSlot = throwable(inventory.getItem(originalSlot)) ? originalSlot : -1;
        if (potionSlot < 0) for (int slot = 0; slot < 9; slot++) {
            if (throwable(inventory.getItem(slot))) { potionSlot = slot; break; }
        }
        if (potionSlot < 0) { setEnabled(false); return; }
        inventory.setSelectedSlot(potionSlot);
        ItemStack stack = client.player.getMainHandItem();
        if (client.player.getCooldowns().isOnCooldown(stack)) {
            if (restoreSlot.get()) inventory.setSelectedSlot(originalSlot);
            return;
        }
        InteractionResult result = client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        if (result instanceof InteractionResult.Success) {
            client.player.swing(InteractionHand.MAIN_HAND, stack.getInteractAnimation(), false);
            client.player.itemUsed(InteractionHand.MAIN_HAND);
        }
        if (restoreSlot.get()) inventory.setSelectedSlot(originalSlot);
        setEnabled(false);
    }

    private static boolean throwable(ItemStack stack) { return stack.getItem() instanceof ThrowablePotionItem; }
}
