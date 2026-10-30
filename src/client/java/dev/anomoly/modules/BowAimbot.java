package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import net.minecraft.client.Minecraft;

public final class BowAimbot extends RangedAim {
    public BowAimbot() {
        super("bow_aimbot", "Bow Aimbot", "Lead visible targets with bow, arrow-crossbow or firework-crossbow flight physics; never fires.", true);
    }

    @Override public void tick(Minecraft client) {
        if (!AnomolyClient.MODULES.get(ProjectileAimbot.class).active(client)) super.tick(client);
    }
}
