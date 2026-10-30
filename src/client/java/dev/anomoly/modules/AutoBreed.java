package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Feeds reachable pairs of adult animals with their own vanilla breeding food. */
public final class AutoBreed extends Module {
    private final StringSetting species = add(new StringSetting("species", "Animal IDs",
        "minecraft:cow,minecraft:sheep,minecraft:pig,minecraft:chicken"));
    private final FloatSetting range = add(new FloatSetting("range", "Feeding range", 3F, 2F, 6F));
    private final FloatSetting partnerRange = add(new FloatSetting("partner_range", "Maximum pair separation", 6F, 2F, 12F));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between feeds", 10F, 2F, 40F));
    private final BooleanSetting lineOfSight = add(new BooleanSetting("line_of_sight", "Require line of sight", true));
    private final BooleanSetting useOffhand = add(new BooleanSetting("use_offhand", "Use offhand food", true));
    private final BooleanSetting autoSelect = add(new BooleanSetting("auto_select", "Select hotbar food", true));
    private final BooleanSetting restoreSlot = add(new BooleanSetting("restore_slot", "Restore previous hotbar slot", true));
    private final Map<UUID, Long> fedUntil = new HashMap<>();
    private ClientLevel observedLevel;
    private LocalPlayer observedPlayer;
    private UUID pendingPartner;
    private Item pendingFood;
    private long pendingUntil;
    private int cooldown;

    public AutoBreed() {
        super("auto_breed", "Auto Breed", "Feed reachable pairs of selected adult animals with their breeding food.", Category.WORLD);
    }

    @Override public void tick(Minecraft client) {
        if (client.gameMode == null || client.gui.screen() != null || !client.player.isAlive()
            || client.player.isSpectator() || client.player.isHandsBusy() || client.player.isUsingItem()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()) return;
        LocalPlayer player = client.player;
        if (observedPlayer != player || observedLevel != client.level) {
            observedPlayer = player;
            observedLevel = client.level;
            fedUntil.clear();
            pendingPartner = null;
            pendingFood = null;
            cooldown = 0;
        }
        long now = client.level.getGameTime();
        fedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (cooldown > 0) { cooldown--; return; }
        List<Animal> nearby = client.level.getEntitiesOfClass(Animal.class,
            player.getBoundingBox().inflate(Math.max(range.get(), partnerRange.get())));
        if (pendingPartner != null) {
            Animal partner = nearby.stream().filter(animal -> animal.getUUID().equals(pendingPartner)).findFirst().orElse(null);
            if (partner != null && !partner.isBaby() && reachable(client, partner)
                && pendingFood != null && feed(client, partner, pendingFood)) {
                fedUntil.put(partner.getUUID(), now + 6000);
                pendingPartner = null;
                pendingFood = null;
                return;
            }
            if (now < pendingUntil) return;
            pendingPartner = null;
            pendingFood = null;
        }
        List<Animal> candidates = new ArrayList<>();
        for (Animal animal : nearby)
            if (!animal.isBaby() && !fedUntil.containsKey(animal.getUUID())
                && allowed(animal) && reachable(client, animal)) candidates.add(animal);
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (int i = 0; i < candidates.size(); i++) {
            Animal first = candidates.get(i);
            for (int j = i + 1; j < candidates.size(); j++) {
                Animal second = candidates.get(j);
                if (first.getClass() != second.getClass()
                    || first.distanceToSqr(second) > partnerRange.get() * partnerRange.get()) continue;
                Item food = sharedFood(player, first, second);
                if (food == null || countFood(player, food) < 2) continue;
                if (feed(client, first, food)) {
                    fedUntil.put(first.getUUID(), now + 6000);
                    pendingPartner = second.getUUID();
                    pendingFood = food;
                    pendingUntil = now + 100;
                }
                return;
            }
        }
    }

    private boolean allowed(Animal animal) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
        for (String raw : species.get().split(",")) {
            String listed = raw.trim();
            if (listed.isEmpty()) continue;
            if (!listed.contains(":")) listed = "minecraft:" + listed;
            if (id.equals(listed)) return true;
        }
        return false;
    }

    private boolean reachable(Minecraft client, Animal animal) {
        Vec3 eyes = client.player.getEyePosition();
        Vec3 hit = animal.getBoundingBox().getCenter();
        if (eyes.distanceToSqr(hit) > range.get() * range.get()
            || !client.player.isWithinEntityInteractionRange(animal, 0)) return false;
        return !lineOfSight.get() || client.level.clip(new ClipContext(eyes, hit,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player)).getType() == HitResult.Type.MISS;
    }

    private Item sharedFood(LocalPlayer player, Animal first, Animal second) {
        if (foodForPair(player.getMainHandItem(), first, second)) return player.getMainHandItem().getItem();
        if (useOffhand.get() && foodForPair(player.getOffhandItem(), first, second)) return player.getOffhandItem().getItem();
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (foodForPair(stack, first, second)) return stack.getItem();
        }
        return null;
    }

    private static boolean foodForPair(ItemStack stack, Animal first, Animal second) {
        return !stack.isEmpty() && first.isFood(stack) && second.isFood(stack);
    }

    private int countFood(LocalPlayer player, Item food) {
        int count = 0;
        if (useOffhand.get() && player.getOffhandItem().is(food)) count += player.getOffhandItem().getCount();
        if (autoSelect.get()) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.is(food)) count += stack.getCount();
            }
        } else if (player.getMainHandItem().is(food)) count += player.getMainHandItem().getCount();
        return count;
    }

    private boolean feed(Minecraft client, Animal animal, Item food) {
        LocalPlayer player = client.player;
        int previousSlot = player.getInventory().getSelectedSlot();
        InteractionHand hand = selectFood(player, food);
        if (hand == null) return false;
        try {
            ItemStack stack = player.getItemInHand(hand);
            Vec3 hit = animal.getBoundingBox().getCenter();
            InteractionResult result = client.gameMode.interact(player, animal, new EntityHitResult(animal, hit), hand);
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
                cooldown = (int)interval.get();
                return true;
            }
            return false;
        } finally {
            if (hand == InteractionHand.MAIN_HAND && restoreSlot.get()
                && player.getInventory().getSelectedSlot() != previousSlot) {
                player.getInventory().setSelectedSlot(previousSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(previousSlot));
            }
        }
    }

    private InteractionHand selectFood(LocalPlayer player, Item food) {
        if (player.getMainHandItem().is(food)) return InteractionHand.MAIN_HAND;
        if (useOffhand.get() && player.getOffhandItem().is(food)) return InteractionHand.OFF_HAND;
        if (autoSelect.get()) for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(food)) {
                player.getInventory().setSelectedSlot(slot);
                return InteractionHand.MAIN_HAND;
            }
        return null;
    }

    @Override protected void onDisable(Minecraft client) {
        observedLevel = null;
        observedPlayer = null;
        fedUntil.clear();
        pendingPartner = null;
        pendingFood = null;
        cooldown = 0;
    }
}
