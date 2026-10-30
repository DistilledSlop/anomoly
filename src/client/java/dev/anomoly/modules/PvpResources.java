package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class PvpResources extends TextHud {
    public PvpResources() {
        super("pvp_resources", "PVPResources", "Count totems, experience bottles, end crystals and enchanted golden apples in your inventory on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var p = client.player;
        return List.of("Totems " + ItemCount.of(p, Items.TOTEM_OF_UNDYING), "XP bottles " + ItemCount.of(p, Items.EXPERIENCE_BOTTLE),
            "Crystals " + ItemCount.of(p, Items.END_CRYSTAL), "E-gaps " + ItemCount.of(p, Items.ENCHANTED_GOLDEN_APPLE));
    }
}
