package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Casts a fishing rod at a target and reels when the hook catches or times out. */
public final class AutoRod extends Module {
    private static final ProjectilePhysics.Profile HOOK_FLIGHT =
        new ProjectilePhysics.Profile(1.1F, 0.03, 0.92F, true, false, false, 0F, 0F);
    private final FloatSetting range = add(new FloatSetting("range", "Range", 9F, 3F, 18F));
    private final FloatSetting fieldOfView = add(new FloatSetting("fov", "FOV", 120F, 20F, 360F));
    private final FloatSetting turnSpeed = add(new FloatSetting("turn_speed", "Turn speed", 25F, 1F, 90F));
    private final FloatSetting lead = add(new FloatSetting("target_lead", "Target movement lead", 1F, 0F, 1.5F));
    private final FloatSetting timeout = add(new FloatSetting("timeout_ticks", "Reel after ticks", 35F, 10F, 80F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between casts", 20F, 5F, 80F));
    private final BooleanSetting targetMobs = add(new BooleanSetting("target_mobs", "Target mobs", false));
    private final BooleanSetting targetInvisible = add(new BooleanSetting("target_invisible", "Target invisible entities", false));
    private final BooleanSetting selectHotbar = add(new BooleanSetting("select_hotbar", "Select rod in hotbar", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private int cooldown;
    private int castTicks;
    private int stableTicks;
    private int targetId = -1;
    private int originalSlot = -1;
    private int rodSlot = -1;
    private InteractionHand rodHand;
    private boolean ownsHook;
    private LocalPlayer observedPlayer;

    public AutoRod() {
        super("auto_rod", "Auto Rod", "Aim, cast, and reel a fishing rod against nearby visible targets.", Category.COMBAT);
    }

    public boolean ownsHook(Minecraft client) { return enabled() && ownsHook && observedPlayer == client.player; }

    public boolean active(Minecraft client) {
        if (!enabled() || client.player == null || client.gameMode == null || client.gui.screen() != null
            || client.player.isSpectator() || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)) return false;
        if (ownsHook) return true;
        if (cooldown > 0 || client.player.fishing != null) return false;
        if (!hasRod(client.player)) return false;
        return AimTargeting.find(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get()) != null;
    }

    @Override public void tick(Minecraft client) {
        if (observedPlayer != client.player) {
            reset();
            observedPlayer = client.player;
        }
        if (cooldown > 0) cooldown--;
        if (client.gameMode == null || client.gui.screen() != null || client.player.isSpectator()) return;
        if (ownsHook) {
            maintainHook(client);
            return;
        }
        if (cooldown > 0 || client.player.fishing != null || client.player.isUsingItem()
            || client.player.isHandsBusy() || client.options.keyUse.isDown()
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)) return;
        if (!hasRod(client.player)) return;
        for (AimTargeting.Solution candidate : AimTargeting.candidates(client, range.get(), fieldOfView.get(), targetMobs.get(), targetInvisible.get())) {
            ProjectileAimSolver.Solution shot = ProjectileAimSolver.solve(client, candidate.entity(), HOOK_FLIGHT, lead.get());
            if (shot == null) continue;
            if (targetId != candidate.entity().getId()) stableTicks = 0;
            targetId = candidate.entity().getId();
            AimTargeting.turn(client, new AimTargeting.Solution(candidate.entity(), shot.yaw(), shot.pitch()), turnSpeed.get(), 0.1F);
            float yawError = Mth.wrapDegrees(shot.yaw() - client.player.getYRot());
            float pitchError = shot.pitch() - client.player.getXRot();
            stableTicks = Math.abs(yawError) <= 2F && Math.abs(pitchError) <= 2F ? stableTicks + 1 : 0;
            if (stableTicks >= 2) cast(client);
            return;
        }
        stableTicks = 0;
        targetId = -1;
    }

    private void cast(Minecraft client) {
        rodHand = selectRod(client.player);
        if (rodHand == null) return;
        ItemStack stack = client.player.getItemInHand(rodHand);
        if (client.player.getCooldowns().isOnCooldown(stack)
            || !(client.gameMode.useItem(client.player, rodHand) instanceof InteractionResult.Success)) {
            restore(client.player);
            return;
        }
        client.player.swing(rodHand, stack.getInteractAnimation(), false);
        client.player.itemUsed(rodHand);
        ownsHook = true;
        castTicks = 0;
        stableTicks = 0;
    }

    private void maintainHook(Minecraft client) {
        castTicks++;
        if (rodHand == null || !client.player.getItemInHand(rodHand).is(Items.FISHING_ROD)
            || rodSlot >= 0 && client.player.getInventory().getSelectedSlot() != rodSlot) {
            finish(client.player);
            return;
        }
        if (client.options.keyUse.isDown()) return;
        var hook = client.player.fishing;
        if (hook == null) {
            if (castTicks > 12) finish(client.player);
            return;
        }
        if (hook.getHookedIn() != null || hook.onGround() || hook.horizontalCollision
            || castTicks >= (int)timeout.get()) {
            ItemStack stack = client.player.getItemInHand(rodHand);
            if (client.gameMode.useItem(client.player, rodHand) instanceof InteractionResult.Success) {
                client.player.swing(rodHand, stack.getInteractAnimation(), false);
                client.player.itemUsed(rodHand);
            }
            finish(client.player);
        }
    }

    private boolean hasRod(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.FISHING_ROD) || player.getOffhandItem().is(Items.FISHING_ROD)) return true;
        if (selectHotbar.get()) for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.FISHING_ROD)) return true;
        return false;
    }

    private InteractionHand selectRod(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.FISHING_ROD)) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(Items.FISHING_ROD)) return InteractionHand.OFF_HAND;
        if (!selectHotbar.get()) return null;
        for (int slot = 0; slot < 9; slot++) {
            if (!player.getInventory().getItem(slot).is(Items.FISHING_ROD)) continue;
            originalSlot = player.getInventory().getSelectedSlot();
            rodSlot = slot;
            player.getInventory().setSelectedSlot(slot);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private void restore(LocalPlayer player) {
        if (restoreSlot.get() && originalSlot >= 0 && player != null
            && player.getInventory().getSelectedSlot() == rodSlot)
            player.getInventory().setSelectedSlot(originalSlot);
        originalSlot = -1;
        rodSlot = -1;
        rodHand = null;
    }

    private void finish(LocalPlayer player) {
        ownsHook = false;
        castTicks = 0;
        stableTicks = 0;
        targetId = -1;
        cooldown = (int)interval.get();
        restore(player);
    }

    private void reset() {
        ownsHook = false;
        castTicks = 0;
        stableTicks = 0;
        targetId = -1;
        cooldown = 0;
        originalSlot = -1;
        rodSlot = -1;
        rodHand = null;
    }

    @Override protected void onDisable(Minecraft client) {
        if (ownsHook && client.player != null && client.gameMode != null && rodHand != null
            && client.player.getItemInHand(rodHand).is(Items.FISHING_ROD) && client.player.fishing != null)
            client.gameMode.useItem(client.player, rodHand);
        restore(client.player);
        reset();
        observedPlayer = null;
    }
}
