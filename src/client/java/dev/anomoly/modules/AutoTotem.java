package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public final class AutoTotem extends Module {
    private final BooleanSetting replaceExisting = add(new BooleanSetting("replace_existing", "Replace offhand item", true));
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 10, 2, 40));
    private int cooldown;

    public AutoTotem() {
        super("auto_totem", "Auto Totem", "Equip a totem from inventory in the offhand.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || player.isUsingItem()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
            || player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
            || !replaceExisting.get() && !player.getOffhandItem().isEmpty()) return;
        var menu = player.inventoryMenu;
        for (int source = 9; source < 45; source++) {
            if (!menu.getSlot(source).getItem().is(Items.TOTEM_OF_UNDYING)) continue;
            boolean occupied = !menu.getSlot(45).getItem().isEmpty();
            int id = menu.containerId;
            client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(id, 45, 0, ContainerInput.PICKUP, player);
            if (occupied) client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
            cooldown = (int)delay.get();
            return;
        }
    }
}
