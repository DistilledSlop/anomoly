package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;

public final class AutoSword extends Module {
    public AutoSword() {
        super("auto_sword", "Auto Sword", "Select the strongest sword in the hotbar for attacks.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || AnomolyClient.MODULES.get(AutoRod.class).active(client)) return;
        if (client.options.keyAttack.isDown() && client.hitResult instanceof EntityHitResult
            || !client.options.keyAttack.isDown() && AnomolyClient.MODULES.get(KillAura.class).enabled()) {
            WeaponSelector.selectBest(client, true);
        }
    }
}
