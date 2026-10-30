package dev.anomoly.mixin;

import dev.anomoly.AnomolyClient;
import dev.anomoly.modules.Velocity;
import dev.anomoly.modules.NoRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("TAIL"))
    private void anomoly$scaleVelocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || packet.id() != client.player.getId()) return;
        Velocity velocity = AnomolyClient.MODULES.get(Velocity.class);
        if (velocity.enabled()) client.player.lerpMotion(velocity.scale(packet.movement()));
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void anomoly$recordCorrection(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        AnomolyClient.PROFILE.correction(packet.id(), Minecraft.getInstance().player);
    }

    @Redirect(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;setYRot(F)V"))
    private static void anomoly$ignoreTeleportYaw(Entity entity, float yaw) {
        if (entity != Minecraft.getInstance().player || !AnomolyClient.MODULES.get(NoRotate.class).ignoreTeleport()) entity.setYRot(yaw);
    }

    @Redirect(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;setXRot(F)V"))
    private static void anomoly$ignoreTeleportPitch(Entity entity, float pitch) {
        if (entity != Minecraft.getInstance().player || !AnomolyClient.MODULES.get(NoRotate.class).ignoreTeleport()) entity.setXRot(pitch);
    }

    @Redirect(method = "setValuesFromPositionPacket", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Entity;setOldPosAndRot(Lnet/minecraft/world/phys/Vec3;FF)V"))
    private static void anomoly$keepOldView(Entity entity, Vec3 position, float yaw, float pitch) {
        if (entity == Minecraft.getInstance().player && AnomolyClient.MODULES.get(NoRotate.class).ignoreTeleport()) {
            entity.setOldPosAndRot(position, entity.getYRot(), entity.getXRot());
        } else entity.setOldPosAndRot(position, yaw, pitch);
    }

    @Redirect(method = "handleRotatePlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;setYRot(F)V"))
    private void anomoly$ignoreRotationYaw(Player player, float yaw) {
        if (!AnomolyClient.MODULES.get(NoRotate.class).ignoreRotationPacket()) player.setYRot(yaw);
    }

    @Redirect(method = "handleRotatePlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;setXRot(F)V"))
    private void anomoly$ignoreRotationPitch(Player player, float pitch) {
        if (!AnomolyClient.MODULES.get(NoRotate.class).ignoreRotationPacket()) player.setXRot(pitch);
    }
}
