package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public final class ExtraTooltips extends Module {
    private final BooleanSetting durability = add(new BooleanSetting("durability", "Exact durability", true));
    private final BooleanSetting id = add(new BooleanSetting("id", "Registry ID", true));

    public ExtraTooltips() {
        super("extra_tooltips", "ExtraTooltips", "Add exact durability and the registry ID to item tooltips (also covers Better Tooltips and Tooltips).", Category.RENDER);
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!enabled()) return;
            if (durability.get() && stack.isDamageableItem()) {
                lines.add(Component.literal("Durability: " + (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage())
                    .withStyle(ChatFormatting.GRAY));
            }
            if (id.get()) lines.add(Component.literal(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
        });
    }
}
