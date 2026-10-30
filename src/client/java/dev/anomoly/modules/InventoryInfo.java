package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class InventoryInfo extends TextHud {
    public InventoryInfo() {
        super("inventory_info", "InventoryInfo", "Show free inventory slots and the total item count on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var inventory = client.player.getInventory();
        int free = 0, items = 0;
        for (int slot = 0; slot < 36; slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) free++;
            else items += stack.getCount();
        }
        return List.of("Inventory: " + free + " free slots, " + items + " items");
    }
}
