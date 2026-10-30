package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

public final class Borders extends Module {
    private final BooleanSetting regions = add(new BooleanSetting("regions", "Also draw region (32 chunk) borders", true));

    public Borders() {
        super("borders", "Borders", "Draw vertical lines along nearby chunk borders, plus region borders.", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        var player = client.player;
        int baseX = (Math.floorDiv((int)Math.floor(player.getX()), 16)) * 16;
        int baseZ = (Math.floorDiv((int)Math.floor(player.getZ()), 16)) * 16;
        double y0 = player.getY() - 6;
        double y1 = player.getY() + 8;
        WorldGizmos.draw(client, () -> {
            for (int dx = -32; dx <= 48; dx += 16) {
                for (int dz = -32; dz <= 48; dz += 16) {
                    int x = baseX + dx;
                    int z = baseZ + dz;
                    boolean region = regions.get() && Math.floorMod(x, 512) == 0 && Math.floorMod(z, 512) == 0;
                    if (!region && (Math.abs(dx - 8) > 24 || Math.abs(dz - 8) > 24)) continue;
                    Gizmos.line(new Vec3(x, y0, z), new Vec3(x, y1, z), region ? 0xFFFF5555 : 0xFF5599FF, region ? 3.0F : 1.5F);
                }
            }
        });
    }
}
