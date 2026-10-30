package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MinecraftAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoBreak extends Module {
    private boolean holdingAttack;
    private BlockPos lastTarget;
    private LocalPlayer observedPlayer;

    public AutoBreak() {
        super("auto_break", "Auto Break", "Mine the solid block under the crosshair without holding attack.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (observedPlayer != client.player) {
            release();
            observedPlayer = client.player;
        }
        if (client.gui.screen() != null || client.gameMode == null || client.player.isSpectator()
            || client.player.isUsingItem() || !(client.hitResult instanceof BlockHitResult hit)
            || client.level.getBlockState(hit.getBlockPos()).isAir()) {
            release();
            return;
        }
        if (!holdingAttack || !hit.getBlockPos().equals(lastTarget)) {
            ((MinecraftAccessor)client).anomoly$startAttack();
            lastTarget = hit.getBlockPos().immutable();
        }
        client.options.keyAttack.setDown(true);
        holdingAttack = true;
    }

    private void release() {
        if (holdingAttack) KeyMapping.setAll();
        holdingAttack = false;
        lastTarget = null;
    }

    @Override protected void onDisable(Minecraft client) {
        release();
        observedPlayer = null;
    }
}
