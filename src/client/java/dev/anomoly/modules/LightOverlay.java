package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public final class LightOverlay extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 12, 4, 24));
    private final List<BlockPos> dark = new ArrayList<>();
    private int ticks;

    public LightOverlay() {
        super("light_overlay", "Light Overlay", "Mark floor blocks where block light is zero and hostile mobs can spawn (also covers MobSpawnEsp).", Category.RENDER);
    }

    @Override protected void onEnable(Minecraft client) { dark.clear(); ticks = 0; }

    @Override public void tick(Minecraft client) {
        if (ticks++ % 10 == 0) scan(client);
        WorldGizmos.draw(client, () -> {
            GizmoStyle style = GizmoStyle.stroke(0xFFFF3030, 2.0F);
            for (BlockPos pos : dark) {
                Vec3 base = new Vec3(pos.getX(), pos.getY() + 0.02, pos.getZ());
                Gizmos.rect(base, base.add(1, 0, 1), net.minecraft.core.Direction.UP, style);
            }
        });
    }

    private void scan(Minecraft client) {
        dark.clear();
        int r = (int)range.get();
        BlockPos origin = client.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -4, -r), origin.offset(r, 4, r))) {
            var floor = client.level.getBlockState(pos.below());
            if (client.level.getBlockState(pos).isAir() && floor.isFaceSturdy(client.level, pos.below(), net.minecraft.core.Direction.UP)
                && client.level.getBrightness(LightLayer.BLOCK, pos) == 0 && dark.size() < 2048) dark.add(pos.immutable());
        }
    }
}
