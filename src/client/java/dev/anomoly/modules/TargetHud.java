package dev.anomoly.modules;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public final class TargetHud extends TextHud {
    public TargetHud() {
        super("target_hud", "TargetHud", "Show name, health and distance of the living entity under your crosshair on the HUD.");
    }

    @Override protected List<String> lines(Minecraft client) {
        if (!(client.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)) return List.of();
        return List.of(target.getName().getString(), String.format(Locale.ROOT, "HP %.1f / %.1f  %.1fm",
            target.getHealth() + target.getAbsorptionAmount(), target.getMaxHealth(), client.player.distanceTo(target)));
    }
}
