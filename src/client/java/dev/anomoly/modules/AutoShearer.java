package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Uses ordinary entity interactions to shear nearby adult sheep. */
public final class AutoShearer extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Shearing range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between attempts", 10F, 2F, 40F));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand shears", true));
    private final BooleanSetting autoSelect = add(new BooleanSetting("auto_select", "Select hotbar shears", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AutoShearer() {
        super("auto_shearer", "Auto Shearer", "Shear nearby adult sheep with shears from either hand or the hotbar.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            cooldown = 0;
        }
        if (cooldown > 0) { cooldown--; return; }
        if (!hasShears(player)) return;
        Vec3 eyes = player.getEyePosition();
        Sheep target = client.level.getEntitiesOfClass(Sheep.class,
                player.getBoundingBox().inflate(range.get()), Sheep::readyForShearing).stream()
            .filter(sheep -> reachable(client, eyes, sheep))
            .min(Comparator.comparingDouble(sheep -> sheep.distanceToSqr(player)))
            .orElse(null);
        if (target == null) return;
        int previousSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectShears(player);
        if (hand == null) return;
        try {
            ItemStack shears = player.getItemInHand(hand);
            Vec3 hit = target.getBoundingBox().getCenter();
            InteractionResult result = client.gameMode.interact(player, target, new EntityHitResult(target, hit), hand);
            if (result.consumesAction()) {
                player.swing(hand, shears.getInteractAnimation(), false);
                player.itemUsed(hand);
            }
            cooldown = (int)interval.get();
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != previousSlot) {
                player.getInventory().setSelectedSlot(previousSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(previousSlot));
            }
        }
    }

    private boolean reachable(Minecraft client, Vec3 eyes, Sheep sheep) {
        Vec3 hit = sheep.getBoundingBox().getCenter();
        if (eyes.distanceToSqr(hit) > range.get() * range.get()
            || !client.player.isWithinEntityInteractionRange(sheep, 0)) return false;
        return !lineOfSight.get() || client.level.clip(new ClipContext(eyes, hit,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player)).getType() == HitResult.Type.MISS;
    }

    private boolean hasShears(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.SHEARS)) return true;
        if (useOffhand.get() && player.getOffhandItem().is(Items.SHEARS)) return true;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.SHEARS)) return true;
        return false;
    }

    private InteractionHand selectShears(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.SHEARS)) return InteractionHand.MAIN_HAND;
        if (useOffhand.get() && player.getOffhandItem().is(Items.SHEARS)) return InteractionHand.OFF_HAND;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.SHEARS)) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    @Override protected void onDisable(Minecraft client) {
        observedPlayer = null;
        observedLevel = null;
        cooldown = 0;
    }
}
