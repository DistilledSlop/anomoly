package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Mounts a suitable nearby rideable entity using an ordinary entity interaction. */
public final class AutoMount extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Mount range", 3F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between attempts", 20F, 5F, 60F));
    private final BooleanSetting horses = add(new BooleanSetting("horses", "Mount horses and related animals", true));
    private final BooleanSetting boats = add(new BooleanSetting("boats", "Mount boats and rafts", true));
    private final BooleanSetting minecarts = add(new BooleanSetting("minecarts", "Mount passenger minecarts", true));
    private final BooleanSetting pigs = add(new BooleanSetting("pigs", "Mount saddled pigs", true));
    private final BooleanSetting striders = add(new BooleanSetting("striders", "Mount saddled striders", true));
    private final BooleanSetting requireTame = add(new BooleanSetting("require_tame", "Require tamed horses", true));
    private final BooleanSetting requireSaddle = add(new BooleanSetting("require_saddle", "Require saddle on animals", true));
    private final BooleanSetting emptyHand = add(new BooleanSetting("empty_hand", "Use an empty hand", true));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final Map<UUID, Long> retryAfter = new HashMap<>();
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AutoMount() {
        super("auto_mount", "Auto Mount", "Mount a nearby empty rideable animal, boat, or minecart.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isPassenger()
            || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            retryAfter.clear();
            cooldown = 20;
            return;
        }
        long now = client.level.getGameTime();
        retryAfter.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (cooldown > 0) { cooldown--; return; }
        Entity target = client.level.getEntities(player, player.getBoundingBox().inflate(range.get()),
                entity -> !retryAfter.containsKey(entity.getUUID()) && mountable(client, entity)).stream()
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (target == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectHand(player);
        if (hand == null) return;
        try {
            Vec3 hit = target.getBoundingBox().getCenter();
            InteractionResult result = client.gameMode.interact(player, target,
                new EntityHitResult(target, hit), hand);
            if (result.consumesAction())
                player.swing(hand, player.getItemInHand(hand).getInteractAnimation(), false);
            cooldown = (int)interval.get();
            retryAfter.put(target.getUUID(), now + 100);
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private boolean mountable(Minecraft client, Entity entity) {
        if (!entity.isAlive() || !entity.getPassengers().isEmpty()
            || !client.player.isWithinEntityInteractionRange(entity, 0)) return false;
        if (entity instanceof AbstractHorse horse) {
            if (!horses.get() || horse.isBaby()
                || requireTame.get() && !(horse instanceof Camel) && !horse.isTamed()
                || requireSaddle.get() && !(horse instanceof Llama) && !horse.isSaddled()) return false;
        } else if (entity instanceof AbstractBoat) {
            if (!boats.get() || entity instanceof AbstractChestBoat) return false;
        } else if (entity instanceof Minecart) {
            if (!minecarts.get()) return false;
        } else if (entity instanceof Pig pig) {
            if (!pigs.get() || !pig.isSaddled()) return false;
        } else if (entity instanceof Strider strider) {
            if (!striders.get() || !strider.isSaddled()) return false;
        } else return false;
        Vec3 eyes = client.player.getEyePosition();
        Vec3 hit = entity.getBoundingBox().getCenter();
        if (eyes.distanceToSqr(hit) > range.get() * range.get()) return false;
        return !lineOfSight.get() || client.level.clip(new ClipContext(eyes, hit,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player)).getType() == HitResult.Type.MISS;
    }

    private InteractionHand selectHand(LocalPlayer player) {
        if (!emptyHand.get()) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().isEmpty()) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().isEmpty()) return InteractionHand.MAIN_HAND;
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    @Override protected void onDisable(Minecraft client) {
        observedPlayer = null;
        observedLevel = null;
        retryAfter.clear();
        cooldown = 0;
    }
}
