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

public final class MultiAura extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 3.0F, 2.0F, 6.0F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final FloatSetting maxTargets = add(new FloatSetting("max_targets", "Targets per swing", 3, 1, 8));

    public MultiAura() {
        super("multi_aura", "MultiAura", "Attack several eligible targets in reach on every full-strength swing.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gui.screen() != null || client.gameMode == null || player.isSpectator() || player.isHandsBusy()
            || player.getAttackStrengthScale(0F) < 1F || player.cannotAttackWithItem(player.getMainHandItem(), 0)) return;
        int hits = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (hits >= maxTargets.get()) break;
            if (!(entity instanceof LivingEntity living) || entity == player || !living.isAlive()) continue;
            if (!(living instanceof Player) && !targetMobs.get()) continue;
            if (living instanceof Player other && (!player.canHarmPlayer(other) || AnomolyClient.MODULES.get(AntiBot.class).reject(client, other))) continue;
            if (player.distanceToSqr(living) > range.get() * range.get() || !player.hasLineOfSight(living)) continue;
            client.gameMode.attack(player, living);
            hits++;
        }
        if (hits > 0) {
            player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
            player.connection.send(ServerboundPunchPacket.INSTANCE);
        }
    }
}
