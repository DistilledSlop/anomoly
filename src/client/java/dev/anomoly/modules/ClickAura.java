package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ClickAura extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 3.0F, 2.0F, 6.0F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));

    public ClickAura() {
        super("click_aura", "ClickAura", "Hit the nearest eligible target in reach whenever you press attack, even when not aiming at it.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gui.screen() != null || client.gameMode == null || !client.options.keyAttack.isDown() || player.isSpectator()
            || player.getAttackStrengthScale(0F) < 1F || player.cannotAttackWithItem(player.getMainHandItem(), 0)) return;
        LivingEntity best = null;
        double bestDistance = range.get() * range.get();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || entity == player || !living.isAlive()) continue;
            if (!(living instanceof Player) && !targetMobs.get()) continue;
            if (living instanceof Player other && (!player.canHarmPlayer(other) || AnomolyClient.MODULES.get(AntiBot.class).reject(client, other))) continue;
            double distance = player.distanceToSqr(living);
            if (distance <= bestDistance && player.hasLineOfSight(living)) {
                best = living;
                bestDistance = distance;
            }
        }
        if (best == null) return;
        client.gameMode.attack(player, best);
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
        player.connection.send(ServerboundPunchPacket.INSTANCE);
    }
}
