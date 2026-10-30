package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.AnomolyClient;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;

/** Aims and throws eggs or snowballs using the shared 26.3 trajectory solver. */
public final class AutoShoot extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 24F, 4F, 48F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 120F, 20F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 25F, 1F, 90F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Minimum ticks between throws", 12F, 4F, 60F));
    private final FloatSetting lead = add(new FloatSetting("target_lead", "Target movement lead", 1F, 0F, 1.5F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Target invisible entities", false));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select eggs or snowballs in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private int cooldown;
    private int targetId = -1;
    private int stableTicks;
    private int originalSlot = -1;
    private int autoSlot = -1;
    private LocalPlayer observedPlayer;

    public AutoShoot() {
        super("auto_shoot", "Auto Shoot", "Aim and throw held eggs or snowballs at visible targets with a controlled cadence.", Category.COMBAT);
    }

    public boolean active(Minecraft client) {
        if (!enabled() || SoupUse.busy(client)
            || AnomolyClient.MODULES.get(AutoBuff.class).combatPaused(client)
            || AnomolyClient.MODULES.get(AutoPearl.class).active(client)
            || AnomolyClient.MODULES.get(AutoRod.class).ownsHook(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || client.player == null || client.gameMode == null || client.gui.screen() != null
            || client.player.isSpectator() || client.player.isHandsBusy()
            || client.player.isUsingItem() || client.options.keyUse.isDown()) return false;
        boolean available = throwable(client.player.getMainHandItem()) || throwable(client.player.getOffhandItem());
        if (!available && selectHotbar.get()) {
            for (int slot = 0; slot < 9; slot++) {
                if (throwable(client.player.getInventory().getItem(slot))) { available = true; break; }
            }
        }
        return available && AimTargeting.find(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get()) != null;
    }

    @Override public void tick(Minecraft client) {
        if (observedPlayer != client.player) {
            resetSelection();
            observedPlayer = client.player;
        }
        if (cooldown > 0) cooldown--;
        if (SoupUse.busy(client)
            || AnomolyClient.MODULES.get(AutoBuff.class).combatPaused(client)
            || AnomolyClient.MODULES.get(AutoPearl.class).active(client)
            || AnomolyClient.MODULES.get(AutoRod.class).ownsHook(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || client.gui.screen() != null || client.gameMode == null || client.player.isSpectator()
            || client.player.isUsingItem() || client.player.isHandsBusy() || client.options.keyUse.isDown()) {
            stableTicks = 0;
            restore(client);
            return;
        }
        if (cooldown > 0) { stableTicks = 0; restore(client); return; }
        List<AimTargeting.Solution> candidates = AimTargeting.candidates(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get());
        if (candidates.isEmpty()) { stableTicks = 0; targetId = -1; restore(client); return; }
        InteractionHand hand = selectProjectile(client);
        if (hand == null) { stableTicks = 0; targetId = -1; return; }
        ItemStack stack = client.player.getItemInHand(hand);
        ProjectilePhysics.Profile physics = ProjectilePhysics.forStack(client, stack, false);
        if (physics == null || client.player.getCooldowns().isOnCooldown(stack)) return;
        for (AimTargeting.Solution candidate : candidates) {
            ProjectileAimSolver.Solution solution = ProjectileAimSolver.solve(client, candidate.entity(), physics, lead.get());
            if (solution == null) continue;
            if (candidate.entity().getId() != targetId) stableTicks = 0;
            targetId = candidate.entity().getId();
            AimTargeting.turn(client, new AimTargeting.Solution(candidate.entity(), solution.yaw(), solution.pitch()), turnSpeed.get(), 0.1F);
            float yawError = Mth.wrapDegrees(solution.yaw() - client.player.getYRot());
            float pitchError = solution.pitch() - client.player.getXRot();
            stableTicks = Math.abs(yawError) <= 1.5F && Math.abs(pitchError) <= 1.5F ? stableTicks + 1 : 0;
            if (stableTicks >= 2 && cooldown == 0 && client.gameMode.useItem(client.player, hand) instanceof net.minecraft.world.InteractionResult.Success) {
                client.player.swing(hand, stack.getInteractAnimation(), false);
                client.player.itemUsed(hand);
                cooldown = (int)interval.get();
                stableTicks = 0;
                restore(client);
            }
            return;
        }
        stableTicks = 0;
        targetId = -1;
        restore(client);
    }

    private InteractionHand selectProjectile(Minecraft client) {
        if (autoSlot >= 0 && client.player.getInventory().getSelectedSlot() != autoSlot) resetSelection();
        if (throwable(client.player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (throwable(client.player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (!selectHotbar.get()) return null;
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            if (!throwable(inventory.getItem(slot))) continue;
            originalSlot = inventory.getSelectedSlot();
            autoSlot = slot;
            inventory.setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private static boolean throwable(ItemStack stack) {
        return stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EggItem;
    }

    private void restore(Minecraft client) {
        if (restoreSlot.get() && originalSlot >= 0 && client.player != null
            && client.player.getInventory().getSelectedSlot() == autoSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        autoSlot = -1;
    }

    private void resetSelection() {
        cooldown = 0;
        targetId = -1;
        stableTicks = 0;
        originalSlot = -1;
        autoSlot = -1;
    }

    @Override protected void onDisable(Minecraft client) {
        restore(client);
        resetSelection();
        observedPlayer = null;
    }
}
