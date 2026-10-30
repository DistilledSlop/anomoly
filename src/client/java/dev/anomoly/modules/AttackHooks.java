package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Shared pre/post attack context for melee modules using the normal attack path. */
public final class AttackHooks {
    public record Context(boolean wasSprinting, Vec3 movement, boolean primed) { }

    private AttackHooks() { }

    public static Context before(Player player, Entity target) {
        Minecraft client = Minecraft.getInstance();
        if (player != client.player || !(target instanceof LivingEntity living) || !living.isAlive()) return null;
        boolean wasSprinting = player.isSprinting();
        Vec3 movement = player.getDeltaMovement();
        boolean primed = AnomolyClient.MODULES.get(SuperKnockback.class).prime(client, living);
        return new Context(wasSprinting, movement, primed);
    }

    public static void after(Context context, Player player, Entity target) {
        if (context == null || player != Minecraft.getInstance().player) return;
        Minecraft client = Minecraft.getInstance();
        AnomolyClient.MODULES.get(KeepSprint.class).restore(client, target, context);
        AnomolyClient.MODULES.get(SprintReset.class).restore(client, target, context);
    }
}
