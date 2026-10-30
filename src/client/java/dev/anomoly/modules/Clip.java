package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class Clip extends Module {
    private final FloatSetting up = add(new FloatSetting("vertical", "Vertical blocks (negative is down)", 3.0F, -9.0F, 9.0F));
    private final BooleanSetting go = add(new BooleanSetting("clip_now", "Clip now", false));

    public Clip() {
        super("clip", "Clip", "Move the player straight up or down a few blocks through solid blocks when triggered.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        if (!go.get()) return;
        go.set(false);
        var player = client.player;
        Vec3 target = player.position().add(0, up.get(), 0);
        player.setPos(target.x, target.y, target.z);
        player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
    }
}
