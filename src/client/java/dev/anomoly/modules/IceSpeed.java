package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.Vec3;

public final class IceSpeed extends Module {
    private final FloatSetting speed = add(new FloatSetting("speed", "Ice speed", 0.45F, 0.1F, 1.5F));

    public IceSpeed() {
        super("ice_speed", "IceSpeed", "Run faster while standing on ice.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (client.gui.screen() != null || !player.onGround() || !MoveUtil.moving(player) || !MoveUtil.freeMovement(player)) return;
        if (!player.level().getBlockState(BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ())).is(BlockTags.ICE)) return;
        MoveUtil.setHorizontal(player, MoveUtil.direction(player, speed.get()));
    }
}
