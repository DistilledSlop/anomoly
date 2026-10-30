package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public final class PacketFly extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Step per tick", 0.06F, 0.01F, 0.3F));

    public PacketFly() {
        super("packet_fly", "PacketFly", "Fly by sending small position steps each tick instead of real velocity (also covers Phase).", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null) return;
        player.noPhysics = true;
        player.fallDistance = 0;
        double y = (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keyShift.isDown() ? 1 : 0);
        Vec3 step = MoveUtil.direction(player, speed.get()).add(0, y * speed.get(), 0);
        player.setDeltaMovement(Vec3.ZERO);
        if (step.lengthSqr() < 1.0E-8) {
            step = new Vec3(0, -0.02, 0);
        }
        Vec3 next = player.position().add(step);
        PacketInterceptor.replay(client, new ServerboundMovePlayerPacket.Pos(next, false, false));
        player.setPos(next.x, next.y, next.z);
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.player != null) client.player.noPhysics = false;
    }
}
