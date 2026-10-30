package dev.anomoly.modules;

import java.util.List;
import net.minecraft.client.Minecraft;

public final class Cooldowns extends TextHud {
    public Cooldowns() {
        super("cooldowns", "Cooldowns", "Show your attack-strength percentage and hurt-immunity ticks on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var player = client.player;
        return List.of("Attack " + (int)(player.getAttackStrengthScale(0F) * 100) + "%", "Hurt time " + player.hurtTime);
    }
}
