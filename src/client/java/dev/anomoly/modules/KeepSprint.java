package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Restores sprint and horizontal movement after the local attack slowdown. */
public final class KeepSprint extends Module {
    private final BooleanSetting restoreMotion = add(new BooleanSetting("restore_motion", "Preserve movement speed", true));
    private final BooleanSetting playersOnly = add(new BooleanSetting("players_only", "Only players", false));

    public KeepSprint() {
        super("keep_sprint", "Keep Sprint", "Keep sprint and movement speed through a melee hit.", Category.COMBAT);
    }

    void restore(Minecraft client, Entity target, AttackHooks.Context context) {
        if (!enabled() || !context.wasSprinting() || client.player == null
            || playersOnly.get() && !(target instanceof Player) || client.player.isSprinting()) return;
        if (restoreMotion.get()) {
            Vec3 now = client.player.getDeltaMovement();
            Vec3 before = context.movement();
            client.player.setDeltaMovement(before.x, now.y, before.z);
        }
        client.player.setSprinting(true);
        client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
            ServerboundPlayerCommandPacket.Action.START_SPRINTING));
    }
}
