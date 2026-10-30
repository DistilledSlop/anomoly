package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Restarts sprint after a player hit so the next full-strength hit can knock back. */
public final class SprintReset extends Module {
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Also reset after mob hits", false));
    private final BooleanSetting requireForward = add(new BooleanSetting("require_forward", "Require forward movement", true));

    public SprintReset() {
        super("sprint_reset", "Sprint Reset", "Restart sprint immediately after hitting a player.", Category.COMBAT);
    }

    void restore(Minecraft client, Entity target, AttackHooks.Context context) {
        if (!enabled() || client.player == null || client.player.isSpectator() || client.player.isSprinting()
            || !context.wasSprinting() && !context.primed()
            || !(target instanceof Player) && !targetMobs.get()
            || requireForward.get() && !client.player.input.hasForwardImpulse()) return;
        client.player.setSprinting(true);
        client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
            ServerboundPlayerCommandPacket.Action.START_SPRINTING));
    }
}
