package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public final class AutoArmor extends Module {
    private final FloatSetting delay = add(new FloatSetting("delay_ticks", "Delay (ticks)", 10, 2, 40));
    private final BooleanSetting replaceUtility = add(new BooleanSetting("replace_utility", "Replace utility gear", false));
    private int cooldown;

    public AutoArmor() {
        super("auto_armor", "Auto Armor", "Equip stronger armor from inventory, one piece at a time.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (cooldown > 0) { cooldown--; return; }
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || player.containerMenu != player.inventoryMenu
            || !player.inventoryMenu.getCarried().isEmpty()) return;
        var menu = player.inventoryMenu;
        for (int armorMenuSlot = 5; armorMenuSlot < 9; armorMenuSlot++) {
            EquipmentSlot equipment = switch (armorMenuSlot) {
                case 5 -> EquipmentSlot.HEAD;
                case 6 -> EquipmentSlot.CHEST;
                case 7 -> EquipmentSlot.LEGS;
                default -> EquipmentSlot.FEET;
            };
            ItemStack equipped = menu.getSlot(armorMenuSlot).getItem();
            if (hasBindingCurse(equipped)) continue;
            double bestScore = score(equipped, equipment);
            if (!equipped.isEmpty() && bestScore < 0 && !replaceUtility.get()) continue;
            int bestSlot = -1;
            for (int slot = 9; slot < 45; slot++) {
                ItemStack candidate = menu.getSlot(slot).getItem();
                double candidateScore = score(candidate, equipment);
                if (candidateScore > bestScore + 0.15) {
                    bestScore = candidateScore;
                    bestSlot = slot;
                }
            }
            if (bestSlot < 0) continue;
            int id = menu.containerId;
            // Pick up the replacement, swap it into the armor slot, then return the old piece.
            client.gameMode.handleContainerInput(id, bestSlot, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(id, armorMenuSlot, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(id, bestSlot, 0, ContainerInput.PICKUP, player);
            cooldown = (int)delay.get();
            return;
        }
    }

    static double score(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || hasBindingCurse(stack)) return -1;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot) return -1;
        final double[] points = {0};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute == Attributes.ARMOR) points[0] += modifier.amount();
            else if (attribute == Attributes.ARMOR_TOUGHNESS) points[0] += modifier.amount() * 0.5;
            else if (attribute == Attributes.KNOCKBACK_RESISTANCE) points[0] += modifier.amount() * 4;
        });
        if (points[0] <= 0) return -1;
        for (var entry : stack.getEnchantments().entrySet()) {
            String name = entry.getKey().unwrapKey().map(key -> key.identifier().getPath()).orElse("");
            if (name.equals("protection")) points[0] += entry.getIntValue() * 0.75;
            else if (name.endsWith("_protection")) points[0] += entry.getIntValue() * 0.3;
        }
        if (stack.isDamageableItem()) points[0] += 0.25 * (1.0 - (double)stack.getDamageValue() / stack.getMaxDamage());
        return points[0];
    }

    static boolean hasBindingCurse(ItemStack stack) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().unwrapKey().map(key -> key.identifier().getPath().equals("binding_curse")).orElse(false)) return true;
        }
        return false;
    }
}
