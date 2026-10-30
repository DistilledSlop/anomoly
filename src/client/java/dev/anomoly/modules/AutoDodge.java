package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Predicts arrow and trident paths, then briefly steers sideways with normal input. */
public final class AutoDodge extends Module {
    private final FloatSetting scanRange = add(new FloatSetting("scan_range", "Projectile scan range", 14F, 4F, 24F));
    private final FloatSetting predictTicks = add(new FloatSetting("predict_ticks", "Prediction ticks", 12F, 3F, 20F));
    private final FloatSetting holdTicks = add(new FloatSetting("hold_ticks", "Minimum dodge ticks", 8F, 2F, 16F));
    private final FloatSetting clearance = add(new FloatSetting("clearance", "Dodge clearance", 0.8F, 0.4F, 1.5F));
    private final BooleanSetting safeEdges = add(new BooleanSetting("safe_edges", "Avoid open edges", true));
    private final BooleanSetting overrideInput = add(new BooleanSetting("override_input", "Override conflicting movement", true));
    private LocalPlayer observedPlayer;
    private KeyMapping syntheticKey;
    private Vec3 dodgeDirection;
    private int currentArrow = -1;
    private int remaining;

    public AutoDodge() {
        super("auto_dodge", "Auto Dodge", "Sidestep incoming arrows and tridents before they hit.", Category.COMBAT);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (observedPlayer != player) { release(); observedPlayer = player; }
        if (client.gui.screen() != null || !player.isAlive() || player.isSpectator()
            || player.isPassenger() || player.isFallFlying() || !player.onGround()) { release(); return; }
        Threat threat = closestThreat(client, player);
        if (threat != null) {
            if (currentArrow != threat.arrow().getId() || syntheticKey == null) {
                DodgeChoice chosen = dodgeChoice(client, player, threat.arrow());
                if (chosen == null) { release(); return; }
                setSyntheticKey(client, chosen.key());
                dodgeDirection = chosen.direction();
                currentArrow = threat.arrow().getId();
            }
            remaining = (int)holdTicks.get();
        } else if (remaining > 0) remaining--;
        else { release(); return; }
        if (syntheticKey != null) {
            if (!safeLanding(client, player, dodgeDirection)) { release(); return; }
            if (overrideInput.get()) oppositeKey(client, syntheticKey).setDown(false);
            syntheticKey.setDown(true);
        }
    }

    private Threat closestThreat(Minecraft client, LocalPlayer player) {
        Threat best = null;
        double rangeSq = scanRange.get() * scanRange.get();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractArrow arrow) || !arrow.isAlive() || arrow.isNoPhysics()
                || player.distanceToSqr(arrow) > rangeSq || arrow.getDeltaMovement().lengthSqr() < 0.02) continue;
            int ticks = impactTicks(client, player, arrow);
            if (ticks > 0 && (best == null || ticks < best.ticks())) best = new Threat(arrow, ticks);
        }
        return best;
    }

    private int impactTicks(Minecraft client, LocalPlayer player, AbstractArrow arrow) {
        Vec3 position = arrow.position();
        Vec3 velocity = arrow.getDeltaMovement();
        AABB body = player.getBoundingBox().inflate(0.25);
        Vec3 playerMotion = player.getDeltaMovement();
        for (int tick = 1; tick <= (int)predictTicks.get(); tick++) {
            Vec3 next = position.add(velocity);
            AABB futureBody = body.move(playerMotion.x * tick * 0.5, 0, playerMotion.z * tick * 0.5);
            var impact = futureBody.clip(position, next);
            HitResult terrain = client.level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));
            if (impact.isPresent() && (terrain.getType() == HitResult.Type.MISS
                || position.distanceToSqr(impact.get()) < position.distanceToSqr(terrain.getLocation()))) return tick;
            if (terrain.getType() != HitResult.Type.MISS) return -1;
            position = next;
            velocity = velocity.scale(arrow.isInWater() ? 0.6 : 0.99)
                .add(0, arrow.isNoGravity() ? 0 : -0.05, 0);
        }
        return -1;
    }

    private DodgeChoice dodgeChoice(Minecraft client, LocalPlayer player, AbstractArrow arrow) {
        Vec3 velocity = arrow.getDeltaMovement();
        Vec3 sideways = new Vec3(-velocity.z, 0, velocity.x).normalize();
        if (sideways.lengthSqr() < 0.5) return null;
        if ((arrow.getId() & 1) == 0) sideways = sideways.scale(-1);
        DodgeChoice first = choiceFor(client, player, sideways);
        if (first != null) return first;
        return choiceFor(client, player, sideways.scale(-1));
    }

    private DodgeChoice choiceFor(Minecraft client, LocalPlayer player, Vec3 sideways) {
        KeyMapping key = closestMovementKey(client, player, sideways);
        Vec3 direction = movementDirection(client, player, key);
        if (!safeLanding(client, player, direction)
            || !overrideInput.get() && oppositeKey(client, key).isDown()) return null;
        return new DodgeChoice(key, direction);
    }

    private boolean safeLanding(Minecraft client, LocalPlayer player, Vec3 direction) {
        for (int step = 1; step <= 4; step++) {
            Vec3 offset = direction.scale(clearance.get() * step / 4.0);
            if (!client.level.noCollision(player, player.getBoundingBox().move(offset))) return false;
            if (safeEdges.get()) {
                BlockPos below = BlockPos.containing(player.getX() + offset.x, player.getY() - 0.1,
                    player.getZ() + offset.z);
                if (!client.level.getBlockState(below).isFaceSturdy(client.level, below, Direction.UP)) return false;
            }
        }
        return true;
    }

    private static KeyMapping closestMovementKey(Minecraft client, LocalPlayer player, Vec3 sideways) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        double forwardDot = sideways.dot(forward);
        double leftDot = sideways.dot(left);
        if (Math.abs(forwardDot) >= Math.abs(leftDot))
            return forwardDot >= 0 ? client.options.keyUp : client.options.keyDown;
        return leftDot >= 0 ? client.options.keyLeft : client.options.keyRight;
    }

    private static Vec3 movementDirection(Minecraft client, LocalPlayer player, KeyMapping key) {
        double yaw = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        if (key == client.options.keyUp) return forward;
        if (key == client.options.keyDown) return forward.scale(-1);
        return key == client.options.keyLeft ? left : left.scale(-1);
    }

    private static KeyMapping oppositeKey(Minecraft client, KeyMapping key) {
        if (key == client.options.keyUp) return client.options.keyDown;
        if (key == client.options.keyDown) return client.options.keyUp;
        if (key == client.options.keyLeft) return client.options.keyRight;
        return client.options.keyLeft;
    }

    private void setSyntheticKey(Minecraft client, KeyMapping key) {
        if (syntheticKey != null) KeyMapping.setAll();
        syntheticKey = key;
        if (overrideInput.get()) oppositeKey(client, key).setDown(false);
        key.setDown(true);
    }

    private void release() {
        if (syntheticKey != null) KeyMapping.setAll();
        syntheticKey = null;
        dodgeDirection = null;
        currentArrow = -1;
        remaining = 0;
    }

    @Override protected void onDisable(Minecraft client) {
        release();
        observedPlayer = null;
    }

    private record Threat(AbstractArrow arrow, int ticks) { }
    private record DodgeChoice(KeyMapping key, Vec3 direction) { }
}
