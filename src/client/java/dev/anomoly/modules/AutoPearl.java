package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Follows an opponent's ender-pearl landing with a predicted throw of our own. */
public final class AutoPearl extends Module {
    private final FloatSetting enemyRange = add(new FloatSetting("enemy_range", "Enemy range", 48F, 8F, 96F));
    private final FloatSetting landingRange = add(new FloatSetting("landing_range", "Landing range", 48F, 8F, 96F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 180F, 30F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 30F, 1F, 90F));
    private final FloatSetting maxError = add(new FloatSetting("max_landing_error", "Maximum landing error", 3F, 0.5F, 8F));
    private final FloatSetting minimumHealth = add(new FloatSetting("minimum_health", "Minimum health", 8F, 6F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Minimum ticks between throws", 60F, 10F, 200F));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select pearl in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Follow invisible players", false));
    private final Set<UUID> seen = new HashSet<>();
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private PearlTrajectory.Shot pending;
    private int pendingAge;
    private int stableTicks;
    private int cooldown;

    public AutoPearl() {
        super("auto_pearl", "Auto Pearl", "Predict an enemy pearl's landing and throw your own pearl to follow.", Category.COMBAT);
    }

    public boolean active(Minecraft client) { return enabled() && pending != null && observedPlayer == client.player; }

    @Override public void tick(Minecraft client) {
        if (observedLevel != client.level || observedPlayer != client.player) {
            reset(client.level, client.player);
            markExisting(client);
            return;
        }
        if (seen.size() > 64 && client.player.tickCount % 20 == 0) {
            seen.clear();
            markExisting(client);
        }
        if (cooldown > 0) cooldown--;
        if (client.gameMode == null || client.gui.screen() != null || client.player.isSpectator()
            || SoupUse.busy(client)
            || client.player.getHealth() < minimumHealth.get()) { pending = null; return; }
        if (pending == null && cooldown == 0) detect(client);
        if (pending == null) return;
        if (++pendingAge > 40 || client.player.isUsingItem() || client.player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) { pending = null; return; }
        float yaw = pending.yaw();
        float pitch = pending.pitch();
        float yawError = Mth.wrapDegrees(yaw - client.player.getYRot());
        float pitchError = pitch - client.player.getXRot();
        AimTargeting.turn(client, new AimTargeting.Solution(client.player, yaw, pitch), turnSpeed.get(), 0.1F);
        stableTicks = Math.abs(yawError) <= 2F && Math.abs(pitchError) <= 2F ? stableTicks + 1 : 0;
        if (stableTicks < 2) return;
        InteractionHand hand = selectPearl(client.player);
        if (hand == null) { pending = null; return; }
        var stack = client.player.getItemInHand(hand);
        if (client.player.getCooldowns().isOnCooldown(stack)) { pending = null; return; }
        if (client.gameMode.useItem(client.player, hand) instanceof InteractionResult.Success) {
            client.player.swing(hand, stack.getInteractAnimation(), false);
            client.player.itemUsed(hand);
            cooldown = (int)interval.get();
        }
        pending = null;
        stableTicks = 0;
    }

    private void detect(Minecraft client) {
        if (!hasPearl(client.player)) return;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl) || seen.contains(pearl.getUUID())) continue;
            if (!(pearl.getOwner() instanceof Player enemy)) continue;
            if (enemy == client.player || !enemy.isAlive() || !client.player.canHarmPlayer(enemy)
                || enemy.distanceToSqr(client.player) > enemyRange.get() * enemyRange.get()
                || !targetInvisible.get() && enemy.isInvisible()
                || AnomolyClient.MODULES.get(AntiBot.class).reject(client, enemy)) {
                seen.add(pearl.getUUID());
                continue;
            }
            PearlTrajectory.Landing landing = PearlTrajectory.predict(client, pearl);
            if (landing == null) continue;
            seen.add(pearl.getUUID());
            Vec3 destination = landing.position();
            if (client.player.position().distanceToSqr(destination) > landingRange.get() * landingRange.get()
                || client.player.position().distanceToSqr(destination) < 16) continue;
            float directYaw = (float)(Math.toDegrees(Math.atan2(destination.z - client.player.getZ(),
                destination.x - client.player.getX())) - 90.0);
            if (Math.abs(Mth.wrapDegrees(directYaw - client.player.getYRot())) > fieldOfView.get() / 2F) continue;
            PearlTrajectory.Shot shot = PearlTrajectory.findShot(client, destination, maxError.get());
            if (shot == null) continue;
            pending = shot;
            pendingAge = 0;
            stableTicks = 0;
            return;
        }
    }

    private boolean hasPearl(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.ENDER_PEARL) || player.getOffhandItem().is(Items.ENDER_PEARL)) return true;
        if (selectHotbar.get()) for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) return true;
        return false;
    }

    private InteractionHand selectPearl(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.ENDER_PEARL)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(Items.ENDER_PEARL)) return InteractionHand.OFF_HAND;
        if (!selectHotbar.get()) return null;
        int original = player.getInventory().getSelectedSlot();
        for (int slot = 0; slot < 9; slot++) {
            if (!player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) continue;
            player.getInventory().setSelectedSlot(slot);
            // The use packet carries the selected slot before the throw; restore on a later tick.
            restoreSlotIndex = restoreSlot.get() ? original : -1;
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private int restoreSlotIndex = -1;

    public void finishTick(Minecraft client) {
        if (restoreSlotIndex >= 0 && client.player != null) {
            client.player.getInventory().setSelectedSlot(restoreSlotIndex);
            restoreSlotIndex = -1;
        }
    }

    private void markExisting(Minecraft client) {
        if (client.level == null) return;
        for (var entity : client.level.entitiesForRendering())
            if (entity instanceof ThrownEnderpearl pearl) seen.add(pearl.getUUID());
    }

    private void reset(ClientLevel level, LocalPlayer player) {
        observedLevel = level;
        observedPlayer = player;
        seen.clear();
        pending = null;
        pendingAge = 0;
        stableTicks = 0;
        cooldown = 0;
        restoreSlotIndex = -1;
    }

    @Override protected void onDisable(Minecraft client) {
        finishTick(client);
        reset(null, null);
    }
}
