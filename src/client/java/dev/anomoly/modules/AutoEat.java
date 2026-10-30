package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;

public final class AutoEat extends Module {
    private boolean holdingUse;

    public AutoEat() {
        super("auto_eat", "Auto Eat", "Eat food already held in the main hand when hungry.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        boolean shouldEat = client.gui.screen() == null && !SoupUse.busy(client)
            && client.player.getFoodData().needsFood()
            && client.player.getMainHandItem().has(DataComponents.FOOD);
        if (shouldEat) {
            client.options.keyUse.setDown(true);
            holdingUse = true;
        } else if (holdingUse) {
            KeyMapping.setAll();
            holdingUse = false;
        }
    }

    @Override protected void onDisable(Minecraft client) {
        if (holdingUse) KeyMapping.setAll();
        holdingUse = false;
    }
}
