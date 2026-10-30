package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class AutoDeposit extends Module {
    private final StringSetting ids = add(new StringSetting("item_ids", "Deposit item IDs (comma separated)", ""));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 4, 1, 30));
    private int cooldown;

    public AutoDeposit() {
        super("auto_deposit", "Auto Deposit", "Quick-move specified inventory items into open storage.", Category.PLAYER);
    }

    public Set<String> depositIds() { return StorageMenu.ids(ids.get()); }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        if (client.gameMode == null || !(client.gui.screen() instanceof AbstractContainerScreen<?>)) return;
        var menu = client.player.containerMenu;
        int storageSlots = StorageMenu.storageSlots(menu);
        if (storageSlots == 0 || !menu.getCarried().isEmpty()) return;
        Set<String> selected = depositIds();
        if (selected.isEmpty()) return;
        for (int slot = storageSlots; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || !selected.contains(StorageMenu.id(stack))
                || !StorageMenu.hasStorageSpace(menu, stack)) continue;
            client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
            cooldown = (int)delay.get();
            return;
        }
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
