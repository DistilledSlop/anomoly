package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BlockOutline extends Module {
    public BlockOutline() {
        super("block_outline", "BlockOutline", "Draw a bright colored outline on the block you are looking at (also covers BlockHighLight and Block Selection).", Category.RENDER);
    }

    @Override public void tick(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        WorldGizmos.draw(client, () -> Gizmos.cuboid(hit.getBlockPos(), GizmoStyle.strokeAndFill(0xFF55DDBB, 2.0F, 0x3355DDBB)));
    }
}
