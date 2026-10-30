package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClickTp extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Max distance", 8.0F, 2.0F, 9.0F));
    private boolean wasDown;

    public ClickTp() {
        super("click_tp", "ClickTp", "Right-click with an empty hand to move to the block you are looking at, within a short range.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        boolean down = client.options.keyUse.isDown();
        boolean click = down && !wasDown;
        wasDown = down;
        var player = client.player;
        if (!click || client.gui.screen() != null || !player.getMainHandItem().isEmpty()) return;
        HitResult hit = player.pick(range.get(), 0F, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return;
        Vec3 landing = Vec3.atBottomCenterOf(block.getBlockPos().relative(block.getDirection()));
        player.setPos(landing.x, landing.y, landing.z);
    }
}
