package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TpAura extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Teleport range", 8.0F, 4.0F, 9.0F));

    public TpAura() {
        super("tp_aura", "TpAura", "Report a hop next to a distant player, hit them, and report a hop back, without moving your visible position.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        if (client.gui.screen() != null || client.gameMode == null || player.getAttackStrengthScale(0F) < 1F
            || player.cannotAttackWithItem(player.getMainHandItem(), 0)) return;
        Player target = null;
        for (Player other : client.level.players()) {
            double distance = player.distanceTo(other);
            if (other == player || distance <= 3.0 || distance > range.get() || !other.isAlive() || player.isAlliedTo(other)
                || AnomolyClient.MODULES.get(AntiBot.class).reject(client, other)) continue;
            if (target == null || distance < player.distanceTo(target)) target = other;
        }
        if (target == null) return;
        Vec3 home = player.position();
        Vec3 beside = target.position().subtract(target.position().subtract(home).normalize().scale(1.5));
        PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(beside, false, false));
        client.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
        PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(home, player.onGround(), false));
    }
}
