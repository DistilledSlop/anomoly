package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Descend short ledges sooner without altering a jump or an unknown landing. */
public final class ReverseStep extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Mode", "Instant", "Instant", "Strict", "Accelerator"));
    private final FloatSetting maximumFallDistance = add(new FloatSetting("maximum_fall_distance", "Maximum fall distance", 1.0F, 1.0F, 50.0F));
    private final IntSetting ticks = add(new IntSetting("ticks", "Instant simulation ticks", 20, 1, 40));
    private final BooleanSetting simulateFalling = add(new BooleanSetting("simulate_falling", "Gradual packet-safe descent", false));
    private final FloatSetting strictMotion = add(new FloatSetting("strict_motion", "Strict downward motion", 1.0F, 0.1F, 5.0F));
    private final FloatSetting acceleratorFactor = add(new FloatSetting("accelerator_factor", "Acceleration factor", 1.0F, 0.1F, 5.0F));
    private boolean initiatedJump;

    public ReverseStep() {
        super("reverse_step", "ReverseStep", "Descend nearby ledges faster in Instant, Strict, or Accelerator mode.", Category.MOVEMENT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        if (velocity.y > 0.01) initiatedJump = true;
        if (player.onGround()) {
            initiatedJump = false;
            return;
        }
        if (initiatedJump || client.gui.screen() != null || client.options.keyJump.isDown()
            || player.isPassenger() || player.getAbilities().flying || player.isFallFlying()
            || player.isInWater() || player.isInLava() || player.onClimbable()
            || velocity.y > 0 || player.fallDistance > maximumFallDistance.get()) return;

        double landingY = findLanding(client, player);
        if (Double.isNaN(landingY)) return;
        if (hasHazardBelow(player, landingY)) return;

        switch (mode.get()) {
            case "Instant" -> instant(player, landingY, velocity);
            case "Strict" -> player.setDeltaMovement(velocity.x, -strictMotion.get(), velocity.z);
            case "Accelerator" -> {
                if (velocity.y < 0) player.setDeltaMovement(velocity.x, velocity.y * acceleratorFactor.get(), velocity.z);
            }
            default -> { }
        }
    }

    private double findLanding(Minecraft client, LocalPlayer player) {
        AABB box = player.getBoundingBox();
        double limit = maximumFallDistance.get();
        if (limit < 0.2) return Double.NaN;
        double lastClear = 0;
        for (double drop = 0.05; drop <= limit + 0.05; drop += 0.05) {
            double next = Math.min(drop, limit);
            if (!client.level.noCollision(player, box.move(0, -next, 0))) {
                double low = lastClear;
                double high = next;
                for (int i = 0; i < 8; i++) {
                    double middle = (low + high) * 0.5;
                    if (client.level.noCollision(player, box.move(0, -middle, 0))) low = middle;
                    else high = middle;
                }
                return low >= 0.18 ? box.minY - low : Double.NaN;
            }
            lastClear = next;
            if (next >= limit) break;
        }
        return Double.NaN;
    }

    private static boolean hasHazardBelow(LocalPlayer player, double landingY) {
        AABB box = player.getBoundingBox();
        int low = Mth.floor(landingY - 0.01);
        int high = Mth.floor(box.minY);
        for (int x = Mth.floor(box.minX + 1.0E-5); x <= Mth.floor(box.maxX - 1.0E-5); x++) {
            for (int z = Mth.floor(box.minZ + 1.0E-5); z <= Mth.floor(box.maxZ - 1.0E-5); z++) {
                for (int y = low; y <= high; y++) {
                    BlockState block = player.level().getBlockState(new BlockPos(x, y, z));
                    if (!block.getFluidState().isEmpty() || block.is(Blocks.COBWEB)
                        || block.is(Blocks.POWDER_SNOW) || block.is(Blocks.HAY_BLOCK)
                        || block.is(Blocks.SLIME_BLOCK)) return true;
                }
            }
        }
        return false;
    }

    private void instant(LocalPlayer player, double landingY, Vec3 velocity) {
        if (simulateFalling.get()) {
            double remaining = player.getY() - landingY;
            int steps = Math.min(ticks.get(), Math.max(1, Mth.ceil(remaining / 0.35)));
            double motion = remaining / steps;
            if (motion <= 0.75) player.setDeltaMovement(velocity.x, -motion, velocity.z);
            return;
        }
        double predictedY = player.getY();
        double vertical = velocity.y;
        for (int i = 0; i < ticks.get(); i++) {
            vertical = (vertical - 0.08) * 0.98;
            predictedY = Math.max(landingY, predictedY + vertical);
            if (predictedY <= landingY + 1.0E-4) {
                player.setPos(player.getX(), landingY, player.getZ());
                player.setDeltaMovement(velocity.x, 0, velocity.z);
                return;
            }
        }
    }

    @Override protected void onDisable(Minecraft client) {
        initiatedJump = false;
    }
}
