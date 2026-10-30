package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

public final class FightBot extends Module {
    private final FloatSetting search = add(new FloatSetting("search", "Search radius", 16, 4, 48));

    public FightBot() {
        super("fight_bot", "FightBot", "Walk up to the nearest hostile mob and fight it, jumping over obstacles on the way.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        LivingEntity target = null;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && entity instanceof Enemy && living.isAlive() && player.distanceToSqr(living) < search.get() * search.get()
                && (target == null || player.distanceToSqr(living) < player.distanceToSqr(target))) target = living;
        }
        boolean walk = target != null && client.gui.screen() == null;
        if (walk) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double dy = target.getEyeY() - player.getEyeY();
            player.setYRot((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
            player.setXRot((float)-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
            if (player.distanceTo(target) <= 3.0F && player.getAttackStrengthScale(0F) >= 1F && client.gameMode != null) {
                client.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
            }
            walk = player.distanceTo(target) > 2.2F;
        }
        client.options.keyUp.setDown(walk);
        client.options.keyJump.setDown(walk && player.horizontalCollision);
    }

    @Override protected void onDisable(Minecraft client) { KeyMapping.setAll(); }
}
