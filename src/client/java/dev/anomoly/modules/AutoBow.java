package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Uses normal item interactions to charge, aim, and fire ranged weapons. */
public final class AutoBow extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 48F, 4F, 64F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 140F, 20F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 20F, 1F, 90F));
    private final FloatSetting lead = add(new FloatSetting("target_lead", "Target movement lead", 1F, 0F, 1.5F));
    private final FloatSetting bowCharge = add(new FloatSetting("bow_charge_ticks", "Bow charge ticks", 20F, 3F, 20F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Minimum ticks between shots", 8F, 2F, 60F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Target invisible entities", false));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select weapon in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final BooleanSetting useTrident = add(new BooleanSetting("trident", "Use tridents", true));
    private int cooldown;
    private int reloadDelay;
    private int stableTicks;
    private int targetId = -1;
    private int originalSlot = -1;
    private int autoSlot = -1;
    private boolean autoUsing;
    private boolean syntheticUse;
    private LocalPlayer observedPlayer;

    public AutoBow() {
        super("auto_bow", "Auto Bow", "Select, aim, charge, and fire bows, crossbows, or tridents at visible targets.", Category.COMBAT);
    }

    public boolean active(Minecraft client) {
        if (!enabled() || client.player == null || client.gameMode == null || client.gui.screen() != null
            || client.player.isSpectator()) return false;
        if (autoUsing) return true;
        if (AimTargeting.find(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get()) == null) return false;
        if (available(client.player, client.player.getMainHandItem()) || available(client.player, client.player.getOffhandItem())) return true;
        if (selectHotbar.get()) for (int slot = 0; slot < 9; slot++)
            if (available(client.player, client.player.getInventory().getItem(slot))) return true;
        return false;
    }

    @Override public void tick(Minecraft client) {
        if (observedPlayer != client.player) {
            reset();
            observedPlayer = client.player;
        }
        if (cooldown > 0) cooldown--;
        if (reloadDelay > 0) reloadDelay--;
        if (client.gui.screen() != null || client.gameMode == null || client.player.isSpectator()
            || client.player.isHandsBusy() || client.options.keyUse.isDown()) {
            stableTicks = 0;
            if (!autoUsing) restore(client);
            return;
        }
        // A draw started by the player belongs to the player.
        if (client.player.isUsingItem() && !autoUsing) { restore(client); return; }
        if (autoUsing && !client.player.isUsingItem()) autoUsing = false;
        if (autoSlot >= 0 && client.player.getInventory().getSelectedSlot() != autoSlot) {
            if (autoUsing) client.gameMode.releaseUsingItem(client.player);
            reset();
            return;
        }
        List<AimTargeting.Solution> candidates = AimTargeting.candidates(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get());
        if (candidates.isEmpty()) {
            stableTicks = 0;
            targetId = -1;
            if (autoUsing && client.player.getTicksUsingItem() < 60) { holdUse(client); return; }
            if (autoUsing) release(client);
            restore(client);
            return;
        }
        InteractionHand hand = autoUsing ? client.player.getUsedItemHand() : selectWeapon(client);
        if (hand == null) return;
        ItemStack stack = client.player.getItemInHand(hand);
        if (stack.isEmpty() || !weapon(stack)) { restore(client); return; }
        boolean crossbow = stack.getItem() instanceof CrossbowItem;
        boolean charged = crossbow && CrossbowItem.isCharged(stack);
        ProjectilePhysics.Profile physics = stack.getItem() instanceof BowItem
            && (!autoUsing || client.player.getTicksUsingItem() < (int)bowCharge.get())
            ? ProjectilePhysics.atFullCharge(client, stack) : ProjectilePhysics.forStack(client, stack, false);
        if (physics == null) { holdUse(client); return; }
        ProjectileAimSolver.Solution shot = null;
        for (AimTargeting.Solution candidate : candidates) {
            shot = ProjectileAimSolver.solve(client, candidate.entity(), physics, lead.get());
            if (shot == null) continue;
            if (targetId != candidate.entity().getId()) stableTicks = 0;
            targetId = candidate.entity().getId();
            AimTargeting.turn(client, new AimTargeting.Solution(candidate.entity(), shot.yaw(), shot.pitch()), turnSpeed.get(), 0.1F);
            float yawError = Mth.wrapDegrees(shot.yaw() - client.player.getYRot());
            float pitchError = shot.pitch() - client.player.getXRot();
            stableTicks = Math.abs(yawError) <= 1.5F && Math.abs(pitchError) <= 1.5F ? stableTicks + 1 : 0;
            break;
        }
        if (shot == null) {
            stableTicks = 0;
            if (autoUsing && client.player.getTicksUsingItem() >= 60) { release(client); restore(client); }
            else if (autoUsing) holdUse(client);
            else restore(client);
            return;
        }
        if (autoUsing) {
            int held = client.player.getTicksUsingItem();
            int required = stack.getItem() instanceof BowItem ? (int)bowCharge.get()
                : crossbow ? CrossbowItem.getChargeDuration(stack, client.player) + 1 : 10;
            if (held >= required && (crossbow || stableTicks >= 2)) {
                release(client);
                if (crossbow) reloadDelay = 4;
                else { cooldown = (int)interval.get(); restore(client); }
            } else holdUse(client);
            return;
        }
        if (cooldown > 0 || reloadDelay > 0 || client.player.getCooldowns().isOnCooldown(stack)) return;
        if (crossbow && charged) {
            if (stableTicks >= 2 && client.gameMode.useItem(client.player, hand) instanceof InteractionResult.Success) {
                client.player.swing(hand, stack.getInteractAnimation(), false);
                client.player.itemUsed(hand);
                cooldown = (int)interval.get();
                stableTicks = 0;
                restore(client);
            }
        } else if (available(client.player, stack)
            && client.gameMode.useItem(client.player, hand) instanceof InteractionResult.Success
            && client.player.isUsingItem()) {
            autoUsing = true;
            holdUse(client);
        }
    }

    /** Remove the temporary held-use state after vanilla handles keybindings. */
    public void finishTick(Minecraft client) {
        if (syntheticUse) {
            client.options.keyUse.setDown(false);
            syntheticUse = false;
        }
    }

    private void holdUse(Minecraft client) {
        if (autoUsing && client.player.isUsingItem() && !client.options.keyUse.isDown()) {
            client.options.keyUse.setDown(true);
            syntheticUse = true;
        }
    }

    private void release(Minecraft client) {
        if (client.player != null && client.player.isUsingItem() && client.gameMode != null)
            client.gameMode.releaseUsingItem(client.player);
        autoUsing = false;
        stableTicks = 0;
        // The END_CLIENT_TICK hook removes any synthetic key press from this tick.
    }

    private InteractionHand selectWeapon(Minecraft client) {
        if (available(client.player, client.player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (available(client.player, client.player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (!selectHotbar.get()) return null;
        for (int slot = 0; slot < 9; slot++) {
            if (!available(client.player, client.player.getInventory().getItem(slot))) continue;
            originalSlot = client.player.getInventory().getSelectedSlot();
            autoSlot = slot;
            client.player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private boolean available(LocalPlayer player, ItemStack stack) {
        if (!weapon(stack)) return false;
        if (stack.getItem() instanceof TridentItem) return useTrident.get() && !stack.nextDamageWillBreak()
            && EnchantmentHelper.getTridentSpinAttackStrength(stack, player) <= 0F;
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)
            || !player.getProjectile(stack).isEmpty();
    }

    private static boolean weapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem
            || stack.getItem() instanceof TridentItem;
    }

    private void restore(Minecraft client) {
        if (restoreSlot.get() && originalSlot >= 0 && client.player != null
            && client.player.getInventory().getSelectedSlot() == autoSlot)
            client.player.getInventory().setSelectedSlot(originalSlot);
        originalSlot = -1;
        autoSlot = -1;
    }

    private void reset() {
        cooldown = 0;
        reloadDelay = 0;
        stableTicks = 0;
        targetId = -1;
        originalSlot = -1;
        autoSlot = -1;
        autoUsing = false;
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.player != null && autoUsing) release(client);
        restore(client);
        finishTick(client);
        reset();
        observedPlayer = null;
    }
}
