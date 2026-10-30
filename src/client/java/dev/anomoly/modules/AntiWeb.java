package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class AntiWeb extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Air", "Air", "Strafe"));
    private final FloatSetting strength = add(new FloatSetting("strength", "Strafe strength", 0.23F, 0.01F, 0.8F));
    private final BooleanSetting motionY = add(new BooleanSetting("motion_y", "Override vertical motion", false));
    private final FloatSetting verticalStrength = add(new FloatSetting("vertical_strength", "Vertical strength", 0.6F, -2.0F, 2.0F));
    private final BooleanSetting onlyOnGround = add(new BooleanSetting("only_on_ground", "Strafe only on ground", false));

    public AntiWeb() {
        super("anti_web", "Anti Web", "Ignore or counter cobweb movement slowdown.", Category.MOVEMENT);
    }

    public boolean airMode() {
        return enabled() && mode.get().equals("Air");
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!mode.get().equals("Strafe") || client.gui.screen() != null || !inWeb(player)
            || onlyOnGround.get() && !player.onGround()) return;
        Vec2 input = player.input.getMoveVector();
        Vec3 velocity = player.getDeltaMovement();
        double radians = Math.toRadians(player.getYRot());
        double dx = input.x * Math.cos(radians) - input.y * Math.sin(radians);
        double dz = input.x * Math.sin(radians) + input.y * Math.cos(radians);
        if (input.lengthSquared() > 1.0E-4F) {
            player.setDeltaMovement(dx * strength.get(), motionY.get() ? verticalStrength.get() : velocity.y,
                dz * strength.get());
        } else if (motionY.get()) {
            player.setDeltaMovement(velocity.x, verticalStrength.get(), velocity.z);
        }
    }

    private static boolean inWeb(LocalPlayer player) {
        AABB box = player.getBoundingBox();
        for (int x = Mth.floor(box.minX + 1.0E-5); x <= Mth.floor(box.maxX - 1.0E-5); x++) {
            for (int y = Mth.floor(box.minY + 1.0E-5); y <= Mth.floor(box.maxY - 1.0E-5); y++) {
                for (int z = Mth.floor(box.minZ + 1.0E-5); z <= Mth.floor(box.maxZ - 1.0E-5); z++) {
                    if (player.level().getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB)) return true;
                }
            }
        }
        return false;
    }
}
