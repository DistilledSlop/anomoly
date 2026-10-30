package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class ChestCleaner extends Module {
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 5, 1, 30));
    private final StringSetting discard = add(new StringSetting("discard_ids", "Discard item IDs (comma separated)", ""));
    private int cooldown;

    public ChestCleaner() {
        super("chest_cleaner", "Chest Cleaner", "Drop only specified item types from open storage.", Category.PLAYER);
    }

    public Set<String> discardIds() { return StorageMenu.ids(discard.get()); }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        if (client.gameMode == null || !(client.gui.screen() instanceof AbstractContainerScreen<?>)) return;
        var menu = client.player.containerMenu;
        int sourceSlots = StorageMenu.storageSlots(menu);
        if (sourceSlots == 0 || !menu.getCarried().isEmpty()) return;
        Set<String> selected = discardIds();
        if (selected.isEmpty()) return;
        for (int slot = 0; slot < sourceSlots; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || !selected.contains(StorageMenu.id(stack))) continue;
            client.gameMode.handleContainerInput(menu.containerId, slot, 1, ContainerInput.THROW, client.player);
            cooldown = (int)delay.get();
            return;
        }
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
