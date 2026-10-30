package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public final class AutoReplenish extends Module {
    private final FloatSetting threshold = add(new FloatSetting("threshold", "Refill below count", 8, 1, 64));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 5, 1, 40));
    private final BooleanSetting offhand = add(new BooleanSetting("offhand", "Refill offhand", true));
    private final ItemStack[] remembered = new ItemStack[10];
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AutoReplenish() {
        super("auto_replenish", "Auto Replenish", "Refill low or emptied hotbar and offhand stacks from inventory.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player) {
            Arrays.fill(remembered, null);
            observedPlayer = player;
            cooldown = 0;
        }
        if (client.gameMode == null || client.gui.screen() != null || player.isUsingItem()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) return;
        var menu = player.inventoryMenu;
        for (int index = 0; index < remembered.length; index++) {
            if (index == 9 && !offhand.get()) continue;
            int destination = index == 9 ? 45 : 36 + index;
            ItemStack target = menu.getSlot(destination).getItem();
            if (!target.isEmpty()) remembered[index] = target.copy();
            if (cooldown > 0 || target.getCount() >= threshold.get()
                || !target.isEmpty() && target.getCount() >= target.getMaxStackSize()) continue;
            ItemStack template = target.isEmpty() ? remembered[index] : target;
            if (template == null || template.isEmpty() || template.getMaxStackSize() <= 1) continue;
            for (int source = 9; source < 36; source++) {
                ItemStack available = menu.getSlot(source).getItem();
                if (available.isEmpty() || !ItemStack.isSameItemSameComponents(template, available)) continue;
                int id = menu.containerId;
                client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
                client.gameMode.handleContainerInput(id, destination, 0, ContainerInput.PICKUP, player);
                if (!menu.getCarried().isEmpty()) client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
                cooldown = (int)delay.get();
                return;
            }
        }
        if (cooldown > 0) cooldown--;
    }

    @Override protected void onDisable(Minecraft client) {
        Arrays.fill(remembered, null);
        observedPlayer = null;
        cooldown = 0;
    }
}
