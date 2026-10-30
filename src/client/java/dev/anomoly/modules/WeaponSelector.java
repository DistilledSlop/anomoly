package dev.anomoly.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

final class WeaponSelector {
    private WeaponSelector() { }

    static void selectBest(Minecraft client, boolean swordsOnly) {
        var inventory = client.player.getInventory();
        int best = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.has(DataComponents.WEAPON)) continue;
            if (swordsOnly && !BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_sword")) continue;
            double score = score(stack);
            if (score > bestScore + 0.01 || score >= bestScore - 0.01 && slot == inventory.getSelectedSlot()) {
                best = slot;
                bestScore = score;
            }
        }
        if (best >= 0 && best != inventory.getSelectedSlot()) inventory.setSelectedSlot(best);
    }

    private static double score(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double damage = modifiers.compute(Attributes.ATTACK_DAMAGE, 2.0, EquipmentSlot.MAINHAND);
        double speed = modifiers.compute(Attributes.ATTACK_SPEED, 4.0, EquipmentSlot.MAINHAND);
        double enchantmentBonus = 0;
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().unwrapKey().map(key -> key.identifier().getPath().equals("sharpness")).orElse(false)) {
                enchantmentBonus += entry.getIntValue() * 0.5;
            }
        }
        return (damage + enchantmentBonus) * (0.35 + Math.max(0.1, speed) / 4.0);
    }
}
