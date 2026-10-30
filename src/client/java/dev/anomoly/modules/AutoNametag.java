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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Names nearby unnamed living entities with a named tag from either hand or hotbar. */
public final class AutoNametag extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Naming range", 4.5F, 2F, 6F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between attempts", 10F, 2F, 60F));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand name tags", true));
    private final BooleanSetting autoSelect = add(new BooleanSetting("auto_select", "Select hotbar name tags", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final Map<UUID, Long> retryAfter = new HashMap<>();
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private int cooldown;

    public AutoNametag() {
        super("auto_nametag", "Auto Nametag", "Use named tags on every nearby unnamed living entity.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            retryAfter.clear();
            cooldown = 0;
        }
        long now = client.level.getGameTime();
        retryAfter.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (cooldown > 0) { cooldown--; return; }
        if (!hasNamedTag(player)) return;
        Vec3 eyes = player.getEyePosition();
        LivingEntity target = client.level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range.get()),
                entity -> entity != player && entity.isAlive() && entity.getType().canSerialize()
                    && entity.getCustomName() == null && !retryAfter.containsKey(entity.getUUID())).stream()
            .filter(entity -> reachable(client, eyes, entity))
            .min(Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (target == null) return;
        int oldSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectTag(player);
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
            cooldown = (int) interval.get();
            retryAfter.put(target.getUUID(), now + 100);
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != oldSlot) {
                player.getInventory().setSelectedSlot(oldSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(oldSlot));
            }
        }
    }

    private boolean reachable(Minecraft client, Vec3 eyes, LivingEntity entity) {
        Vec3 hit = entity.getBoundingBox().getCenter();
        if (eyes.distanceToSqr(hit) > range.get() * range.get()
            || !client.player.isWithinEntityInteractionRange(entity, 0)) return false;
        return !lineOfSight.get() || client.level.clip(new ClipContext(eyes, hit,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player)).getType() == HitResult.Type.MISS;
    }

    private boolean hasNamedTag(LocalPlayer player) {
        if (namedTag(player.getMainHandItem())) return true;
        if (useOffhand.get() && namedTag(player.getOffhandItem())) return true;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (namedTag(player.getInventory().getItem(slot))) return true;
        return false;
    }

    private InteractionHand selectTag(LocalPlayer player) {
        if (namedTag(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (useOffhand.get() && namedTag(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (namedTag(player.getInventory().getItem(slot))) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    private static boolean namedTag(ItemStack stack) {
        return stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME);
    }

    @Override protected void onDisable(Minecraft client) {
        observedPlayer = null;
        observedLevel = null;
        retryAfter.clear();
        cooldown = 0;
    }
}
