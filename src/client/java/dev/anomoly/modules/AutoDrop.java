package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class AutoDrop extends Module {
    private final StringSetting ids = add(new StringSetting("item_ids", "Drop item IDs (comma separated)", ""));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 5, 1, 40));
    private final BooleanSetting offhand = add(new BooleanSetting("offhand", "Include offhand", false));
    private int cooldown;

    public AutoDrop() {
        super("auto_drop", "Auto Drop", "Drop specified unwanted items from inventory.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || player.isUsingItem()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) return;
        Set<String> selected = StorageMenu.ids(ids.get());
        if (selected.isEmpty()) return;
        var menu = player.inventoryMenu;
        int end = offhand.get() ? 46 : 45;
        for (int slot = 9; slot < end; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || !selected.contains(StorageMenu.id(stack))) continue;
            client.gameMode.handleContainerInput(menu.containerId, slot, 1, ContainerInput.THROW, player);
            cooldown = (int)delay.get();
            return;
        }
    }

    @Override protected void onDisable(Minecraft client) { cooldown = 0; }
}
