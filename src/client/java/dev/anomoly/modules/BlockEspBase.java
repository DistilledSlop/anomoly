package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.state.BlockState;

/** Scans loaded blocks around the player on an interval and outlines matches through walls. */
abstract class BlockEspBase extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Scan range", 16, 4, 40));
    private final int color;
    private final List<BlockPos> found = new ArrayList<>();
    private int ticks;

    protected BlockEspBase(String id, String name, String description, Category category, int color) {
        super(id, name, description, category);
        this.color = color;
    }

    protected abstract boolean matches(Minecraft client, BlockPos pos, BlockState state);

    /** Subclasses that outline empty space (holes) override these instead. */
    protected boolean includeAir() { return false; }

    protected boolean matchesAir(Minecraft client, BlockPos pos) { return false; }

    @Override protected void onEnable(Minecraft client) { found.clear(); ticks = 0; }

    @Override public void tick(Minecraft client) {
        if (ticks++ % 10 == 0) scan(client);
        WorldGizmos.draw(client, () -> {
            GizmoStyle style = GizmoStyle.stroke(color, 2.0F);
            for (BlockPos pos : found) Gizmos.cuboid(pos, style).setAlwaysOnTop();
        });
    }

    private void scan(Minecraft client) {
        found.clear();
        int r = (int)range.get();
        BlockPos origin = client.player.blockPosition();
        int minY = Math.max(client.level.getMinY(), origin.getY() - r);
        int maxY = Math.min(client.level.getMaxY(), origin.getY() + r);
        for (BlockPos pos : BlockPos.betweenClosed(origin.getX() - r, minY, origin.getZ() - r, origin.getX() + r, maxY, origin.getZ() + r)) {
            BlockState state = client.level.getBlockState(pos);
            boolean hit = state.isAir() ? includeAir() && matchesAir(client, pos) : matches(client, pos, state);
            if (hit && found.size() < 2048) found.add(pos.immutable());
        }
    }
}
