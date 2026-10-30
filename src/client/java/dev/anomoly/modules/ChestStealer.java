package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.AnomolyClient;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class ChestStealer extends Module {
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 3, 1, 20));
    private final StringSetting skip = add(new StringSetting("skip_ids", "Skip item IDs (comma separated)", ""));
    private int cooldown;

    public ChestStealer() {
        super("chest_stealer", "Chest Stealer", "Quick-move items from open storage into your inventory.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        if (client.gameMode == null || !(client.gui.screen() instanceof AbstractContainerScreen<?>)) return;
        var menu = client.player.containerMenu;
        int sourceSlots = StorageMenu.storageSlots(menu);
        if (sourceSlots == 0 || !menu.getCarried().isEmpty()) return;
        Set<String> excluded = StorageMenu.ids(skip.get());
        ChestCleaner cleaner = AnomolyClient.MODULES.get(ChestCleaner.class);
        if (cleaner.enabled()) excluded.addAll(cleaner.discardIds());
        for (int slot = 0; slot < sourceSlots; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || excluded.contains(StorageMenu.id(stack)) || !StorageMenu.hasSpace(menu, stack)) continue;
            client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
            cooldown = (int)delay.get();
            return;
        }
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
