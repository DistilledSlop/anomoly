package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One-shot switch between an elytra and a chestplate, including offhand sources. */
public final class ElytraSwap extends Module {
    private static final int CHEST_SLOT = 6;
    private static final int OFFHAND_SLOT = 45;
    private final FloatSetting minimumDurability = add(new FloatSetting(
        "minimum_elytra_durability", "Minimum remaining elytra durability", 10F, 2F, 100F));
    private ItemStack previousChestplate = ItemStack.EMPTY;
    private int previousSlot = -1;

    public ElytraSwap() {
        super("elytra_swap", "Elytra Swap", "Switch between a chestplate and a usable elytra once.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || player.tickCount < 10) return;
        if (client.gui.screen() != null || player.isSpectator() || player.isUsingItem()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) return;

        ItemStack chest = player.inventoryMenu.getSlot(CHEST_SLOT).getItem();
        if (!AutoArmor.hasBindingCurse(chest)) {
            int source = chest.is(Items.ELYTRA) ? chestplateSource(player) : elytraSource(player);
            if (source >= 0) {
                if (!chest.isEmpty() && !chest.is(Items.ELYTRA)) {
                    previousChestplate = chest.copy();
                    previousSlot = source;
                } else if (chest.is(Items.ELYTRA)) {
                    previousChestplate = ItemStack.EMPTY;
                    previousSlot = -1;
                }
                swap(client, player, source, !chest.isEmpty());
            }
        }
        setEnabled(false);
    }

    public void disconnect() {
        if (enabled()) setEnabled(false);
        previousChestplate = ItemStack.EMPTY;
        previousSlot = -1;
    }

    private int elytraSource(LocalPlayer player) {
        var menu = player.inventoryMenu;
        int bestSlot = -1;
        int bestDurability = -1;
        for (int source = 9; source <= OFFHAND_SLOT; source++) {
            if (source == CHEST_SLOT) continue;
            ItemStack item = menu.getSlot(source).getItem();
            if (!item.is(Items.ELYTRA) || AutoArmor.hasBindingCurse(item)) continue;
            int remaining = item.getMaxDamage() - item.getDamageValue();
            if (remaining < minimumDurability.get() || remaining <= bestDurability) continue;
            bestDurability = remaining;
            bestSlot = source;
        }
        return bestSlot;
    }

    private int chestplateSource(LocalPlayer player) {
        var menu = player.inventoryMenu;
        if (previousSlot >= 9 && previousSlot <= OFFHAND_SLOT
            && ItemStack.isSameItemSameComponents(previousChestplate, menu.getSlot(previousSlot).getItem()))
            return previousSlot;
        int bestSlot = -1;
        double bestScore = -1;
        for (int source = 9; source <= OFFHAND_SLOT; source++) {
            ItemStack item = menu.getSlot(source).getItem();
            if (!previousChestplate.isEmpty() && ItemStack.isSameItemSameComponents(previousChestplate, item))
                return source;
            double score = item.is(Items.ELYTRA) ? -1 : AutoArmor.score(item, EquipmentSlot.CHEST);
            if (score <= bestScore) continue;
            bestScore = score;
            bestSlot = source;
        }
        return bestSlot;
    }

    private static void swap(Minecraft client, LocalPlayer player, int source, boolean occupied) {
        int id = player.inventoryMenu.containerId;
        client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(id, CHEST_SLOT, 0, ContainerInput.PICKUP, player);
        if (occupied) client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
    }
}
