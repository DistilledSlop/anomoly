package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class TotemCounter extends TextHud {
    public TotemCounter() {
        super("totem_counter", "TotemCounter", "Show how many totems of undying you carry on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        return List.of("Totems " + ItemCount.of(client.player, Items.TOTEM_OF_UNDYING));
    }
}
