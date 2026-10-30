package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ElytraReplace extends Module {
    private int cooldown;

    public ElytraReplace() {
        super("elytra_replace", "ElytraReplace", "Swap a nearly broken worn elytra for a healthier one from your inventory.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || --cooldown > 0 || player.isFallFlying()) return;
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!worn.is(Items.ELYTRA) || worn.getMaxDamage() - worn.getDamageValue() > 2) return;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(Items.ELYTRA) || stack.getMaxDamage() - stack.getDamageValue() <= 2) continue;
            int menuSlot = slot < 9 ? slot + 36 : slot;
            int container = player.inventoryMenu.containerId;
            client.gameMode.handleContainerInput(container, menuSlot, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(container, 6, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(container, menuSlot, 0, ContainerInput.PICKUP, player);
            cooldown = 40;
            return;
        }
    }
}
