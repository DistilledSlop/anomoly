package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Primes the server's sprint attack bonus before a normal melee attack. */
public final class SuperKnockback extends Module {
    private final FloatSetting minimumStrength = add(new FloatSetting("minimum_strength", "Minimum attack strength", 0.9F, 0.1F, 1F));
    private final BooleanSetting playersOnly = add(new BooleanSetting("players_only", "Only players", false));
    private final BooleanSetting requireForward = add(new BooleanSetting("require_forward", "Require forward movement", false));

    public SuperKnockback() {
        super("super_knockback", "Super Knockback", "Prime sprint knockback before a full-strength melee hit.", Category.COMBAT);
    }

    boolean prime(Minecraft client, LivingEntity target) {
        if (!enabled() || client.player == null || client.player.isSpectator()
            || client.player.isSprinting() || playersOnly.get() && !(target instanceof Player)
            || requireForward.get() && !client.player.input.hasForwardImpulse()
            || client.player.getAttackStrengthScale(0F) < minimumStrength.get()) return false;
        client.player.connection.send(new ServerboundPlayerCommandPacket(client.player,
            ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        client.player.setSprinting(true);
        return true;
    }
}
