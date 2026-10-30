package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ChestAura extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 4.0F, 2.0F, 6.0F));
    private final Set<BlockPos> opened = new HashSet<>();
    private int cooldown;

    public ChestAura() {
        super("chest_aura", "ChestAura", "Open each nearby chest or barrel once; pair it with Chest Stealer to take the contents.", Category.MISC);
    }

    @Override protected void onEnable(Minecraft client) { opened.clear(); cooldown = 0; }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || --cooldown > 0) return;
        var player = client.player;
        int r = (int)Math.ceil(range.get());
        for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-r, -r, -r), player.blockPosition().offset(r, r, r))) {
            var state = client.level.getBlockState(pos);
            if (!(state.getBlock() instanceof ChestBlock) && !(state.getBlock() instanceof BarrelBlock)) continue;
            if (opened.contains(pos) || Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition()) > range.get() * range.get()) continue;
            opened.add(pos.immutable());
            client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            cooldown = 20;
            return;
        }
    }
}
