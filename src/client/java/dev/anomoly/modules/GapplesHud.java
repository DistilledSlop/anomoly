package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class GapplesHud extends TextHud {
    public GapplesHud() {
        super("gapples_hud", "GapplesHud", "Show how many golden and enchanted golden apples you carry on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        return List.of("Gapples " + ItemCount.of(client.player, Items.GOLDEN_APPLE) + " / "
            + ItemCount.of(client.player, Items.ENCHANTED_GOLDEN_APPLE));
    }
}
