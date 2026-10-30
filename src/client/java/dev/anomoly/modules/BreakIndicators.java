package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;

public final class BreakIndicators extends Module {
    public BreakIndicators() {
        super("break_indicators", "Break Indicators", "Tint the block you are mining from red to green as its progress completes (also covers BreakESP and BreakHighLight).", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null) return;
        var access = (MultiPlayerGameModeAccessor)client.gameMode;
        if (!access.anomoly$isDestroying()) return;
        BlockPos pos = access.anomoly$getDestroyBlockPos();
        float progress = Math.min(1F, Math.max(0F, access.anomoly$getDestroyProgress()));
        int color = ARGB.color(255, (int)(255 * (1 - progress)), (int)(255 * progress), 60);
        int fill = ARGB.color(70, (int)(255 * (1 - progress)), (int)(255 * progress), 60);
        WorldGizmos.draw(client, () -> Gizmos.cuboid(pos, GizmoStyle.strokeAndFill(color, 2.0F, fill)).setAlwaysOnTop());
    }
}
