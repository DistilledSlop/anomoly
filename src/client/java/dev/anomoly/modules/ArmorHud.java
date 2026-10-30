package dev.anomoly.modules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorHud extends TextHud {
    public ArmorHud() {
        super("armor_hud", "ArmorHud", "Show worn armor with remaining durability on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        List<String> rows = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = client.player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            rows.add(stack.getHoverName().getString() + (stack.isDamageableItem()
                ? "  " + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() : ""));
        }
        return rows;
    }
}
