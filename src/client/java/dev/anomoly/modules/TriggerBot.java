package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

public final class TriggerBot extends Module {
    public TriggerBot() {
        super("trigger_bot", "Trigger Bot", "Attack the living target under the crosshair at full cooldown.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || client.gameMode == null || client.player.isSpectator()
            || !(client.hitResult instanceof EntityHitResult hit)
            || !(hit.getEntity() instanceof LivingEntity target) || !target.isAlive()
            || client.player.getAttackStrengthScale(0.0F) < 1.0F) return;
        ((MinecraftAccessor) client).anomoly$startAttack();
    }
}
