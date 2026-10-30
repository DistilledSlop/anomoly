package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/** Replaces the rotation other players see without turning the local view. */
abstract class SilentRotation extends Module implements PacketRewriter {
    private int step;

    protected SilentRotation(String id, String name, String description) {
        super(id, name, description, Category.FUN);
    }

    /** Returns {yaw, pitch} for the next movement packet. */
    protected abstract float[] angles(int step, float yaw, float pitch);

    @Override public Packet<?> rewrite(Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket move)) return packet;
        float[] angles = angles(step++, move.getYRot(0F), move.getXRot(0F));
        boolean ground = move.isOnGround();
        boolean collision = move.horizontalCollision();
        if (move.hasPosition()) {
            return new ServerboundMovePlayerPacket.PosRot(new Vec3(move.getX(0), move.getY(0), move.getZ(0)), angles[0], angles[1], ground, collision);
        }
        return new ServerboundMovePlayerPacket.Rot(angles[0], angles[1], ground, collision);
    }
}
