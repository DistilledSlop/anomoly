package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;

public final class ProjectilePuncher extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 4.0F, 2.0F, 6.0F));

    public ProjectilePuncher() {
        super("projectile_puncher", "ProjectilePuncher", "Punch incoming ghast fireballs and shulker bullets back before they reach you.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gameMode == null || client.gui.screen() != null || player.getAttackStrengthScale(0F) < 1F) return;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LargeFireball) && !(entity instanceof ShulkerBullet)) continue;
            if (player.distanceToSqr(entity) > range.get() * range.get()) continue;
            client.gameMode.attack(player, entity);
            player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
            return;
        }
    }
}
