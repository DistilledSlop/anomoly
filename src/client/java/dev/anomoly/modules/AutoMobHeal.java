package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Heals injured vanilla pets, mounts, and iron golems through ordinary interactions. */
public final class AutoMobHeal extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Healing range", 3F, 2F, 6F));
    private final FloatSetting minMissing = add(new FloatSetting("min_missing_health", "Minimum missing health", 2F, 0.5F, 25F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between heals", 10F, 2F, 40F));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final BooleanSetting ownPetsOnly = add(new BooleanSetting("own_pets_only", "Heal only your wolves and cats", true));
    private final BooleanSetting tamedMountsOnly = add(new BooleanSetting("tamed_mounts_only", "Heal only tamed mounts", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand healing items", true));
    private final BooleanSetting autoSelect = add(new BooleanSetting("auto_select", "Select hotbar healing items", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AutoMobHeal() {
        super("auto_mob_heal", "Auto Mob Heal", "Feed injured pets and mounts or repair nearby iron golems.", Category.WORLD);
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
        LivingEntity target = client.level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range.get())).stream()
            .filter(entity -> healable(client, entity) && hasItem(player, foodFor(entity)))
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (target == null) return;
        int previousSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectItem(player, foodFor(target));
        if (hand == null) return;
        try {
            ItemStack stack = player.getItemInHand(hand);
            Vec3 hit = target.getBoundingBox().getCenter();
            InteractionResult result = client.gameMode.interact(player, target,
                new EntityHitResult(target, hit), hand);
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
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

    private boolean healable(Minecraft client, LivingEntity entity) {
        if (!entity.isAlive() || entity.getMaxHealth() - entity.getHealth() < minMissing.get()
            || !client.player.isWithinEntityInteractionRange(entity, 0)) return false;
        if (entity instanceof Wolf wolf) {
            if (!wolf.isTame() || ownPetsOnly.get() && !wolf.isOwnedBy(client.player)) return false;
        } else if (entity instanceof Cat cat) {
            if (!cat.isTame() || ownPetsOnly.get() && !cat.isOwnedBy(client.player)) return false;
        } else if (entity instanceof AbstractHorse horse) {
            if (horse.isVehicle() || tamedMountsOnly.get() && !horse.isTamed()) return false;
        } else if (!(entity instanceof IronGolem)) return false;
        Vec3 eyes = client.player.getEyePosition();
        Vec3 hit = entity.getBoundingBox().getCenter();
        if (eyes.distanceToSqr(hit) > range.get() * range.get()) return false;
        return !lineOfSight.get() || client.level.clip(new ClipContext(eyes, hit,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player)).getType() == HitResult.Type.MISS;
    }

    private static Predicate<ItemStack> foodFor(LivingEntity entity) {
        if (entity instanceof IronGolem) return stack -> stack.is(Items.IRON_INGOT);
        if (entity instanceof Wolf wolf) return stack -> !stack.isEmpty() && wolf.isFood(stack);
        if (entity instanceof Cat cat) return stack -> !stack.isEmpty() && cat.isFood(stack);
        if (entity instanceof AbstractHorse horse) return stack -> !stack.isEmpty() && horse.isFood(stack)
            && (stack.is(Items.WHEAT) || stack.is(Items.SUGAR) || stack.is(Items.APPLE)
                || stack.is(Items.CARROT) || stack.is(Items.RED_MUSHROOM));
        return stack -> false;
    }

    private boolean hasItem(LocalPlayer player, Predicate<ItemStack> valid) {
        if (valid.test(player.getMainHandItem())) return true;
        if (useOffhand.get() && valid.test(player.getOffhandItem())) return true;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (valid.test(player.getInventory().getItem(slot))) return true;
        return false;
    }

    private InteractionHand selectItem(LocalPlayer player, Predicate<ItemStack> valid) {
        if (valid.test(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (useOffhand.get() && valid.test(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (valid.test(player.getInventory().getItem(slot))) {
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
