package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Warns when worn armor reaches a configurable remaining-durability threshold. */
public final class DurabilityAlert extends Module {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };
    private final FloatSetting threshold = add(new FloatSetting("remaining_percent", "Alert below durability (%)", 10F, 1F, 100F));
    private final FloatSetting repeatSeconds = add(new FloatSetting("repeat_seconds", "Repeat after seconds", 60F, 5F, 300F));
    private final BooleanSetting heldItems = add(new BooleanSetting("held_items", "Also alert for held items", false));
    private final int[] nextAlertTick = new int[SLOTS.length];
    private final boolean[] wasLow = new boolean[SLOTS.length];
    private LocalPlayer observedPlayer;
    private int tick;

    public DurabilityAlert() {
        super("durability_alert", "Durability Alert", "Warn when armor durability is low, with optional held-item alerts.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player) {
            reset();
            observedPlayer = player;
        }
        tick++;
        for (int i = 0; i < SLOTS.length; i++) {
            if (i >= 4 && !heldItems.get()) { wasLow[i] = false; continue; }
            ItemStack stack = player.getItemBySlot(SLOTS[i]);
            if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
                wasLow[i] = false;
                continue;
            }
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            float percent = 100F * remaining / stack.getMaxDamage();
            boolean low = percent <= threshold.get();
            if (low && (!wasLow[i] || tick >= nextAlertTick[i])) {
                String message = stack.getHoverName().getString() + " in " + slotName(SLOTS[i])
                    + " has " + remaining + " durability left (" + Math.round(percent) + "%).";
                client.gui.hud.getChat().addClientSystemMessage(
                    Component.literal("[Anomoly] " + message).withStyle(ChatFormatting.RED));
                AnomolyClient.LOGGER.info("Durability Alert: {}", message);
                nextAlertTick[i] = tick + (int)(repeatSeconds.get() * 20F);
            }
            wasLow[i] = low;
        }
    }

    private static String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "helmet slot";
            case CHEST -> "chest slot";
            case LEGS -> "leggings slot";
            case FEET -> "boots slot";
            case MAINHAND -> "main hand";
            case OFFHAND -> "offhand";
            default -> "equipment";
        };
    }

    private void reset() {
        tick = 0;
        for (int i = 0; i < SLOTS.length; i++) {
            wasLow[i] = false;
            nextAlertTick[i] = 0;
        }
    }

    @Override protected void onDisable(Minecraft client) {
        reset();
        observedPlayer = null;
    }
}
