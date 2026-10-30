package dev.anomoly.modules;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

final class StorageMenu {
    private StorageMenu() { }

    static int storageSlots(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu
            || menu instanceof HopperMenu || menu instanceof DispenserMenu) {
            return menu.slots.size() - 36;
        }
        return 0;
    }

    static Set<String> ids(String csv) {
        return Arrays.stream(csv.split(","))
            .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toSet());
    }

    static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    static boolean hasSpace(AbstractContainerMenu menu, ItemStack source) {
        for (int slot = menu.slots.size() - 36; slot < menu.slots.size(); slot++) {
            ItemStack target = menu.getSlot(slot).getItem();
            if (target.isEmpty() || ItemStack.isSameItemSameComponents(target, source)
                && target.getCount() < target.getMaxStackSize()) return true;
        }
        return false;
    }
}
