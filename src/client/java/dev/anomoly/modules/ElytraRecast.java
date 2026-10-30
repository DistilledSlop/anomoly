package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public final class ElytraRecast extends Module {
    public ElytraRecast() {
        super("elytra_recast", "ElytraRecast", "Reopen your elytra automatically whenever you are falling in the air with one equipped.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player.onGround() || player.isFallFlying() || player.isInWater() || player.isInLava() || player.getDeltaMovement().y >= 0
            || player.getAbilities().flying || player.isPassenger()) return;
        if (player.tryToStartFallFlying()) {
            player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }
}
