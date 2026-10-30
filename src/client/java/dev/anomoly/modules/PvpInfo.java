package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public final class PvpInfo extends TextHud {
    public PvpInfo() {
        super("pvp_info", "PvpInfo", "Show your health, totems and golden apples plus the nearest player's health on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        var me = client.player;
        Player nearest = null;
        for (Player other : client.level.players()) {
            if (other != me && (nearest == null || me.distanceToSqr(other) < me.distanceToSqr(nearest))) nearest = other;
        }
        String own = String.format(Locale.ROOT, "HP %.1f  Totems %d  Gapples %d", me.getHealth() + me.getAbsorptionAmount(),
            ItemCount.of(me, Items.TOTEM_OF_UNDYING), ItemCount.of(me, Items.GOLDEN_APPLE) + ItemCount.of(me, Items.ENCHANTED_GOLDEN_APPLE));
        if (nearest == null) return List.of(own);
        return List.of(own, String.format(Locale.ROOT, "%s HP %.1f  %.1fm", nearest.getName().getString(),
            nearest.getHealth() + nearest.getAbsorptionAmount(), me.distanceTo(nearest)));
    }
}
