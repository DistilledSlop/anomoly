package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Keeps configured health and beneficial-effect buffs active with normal item use. */
public final class AutoBuff extends Module {
    private enum Kind { SPLASH, SOUP, HEAD, GAPPLE, DRINK }
    private record Candidate(int slot, Kind kind, int priority, int preference) { }

    private final BooleanSetting soup = add(new BooleanSetting("soup", "Use healing soups", false));
    private final FloatSetting soupHealth = add(new FloatSetting("soup_health_percent", "Soup health percent", 40, 1, 100));
    private final BooleanSetting soupAbsorption = add(new BooleanSetting("soup_absorption", "Count absorption for soup", true));
    private final BooleanSetting dropBowl = add(new BooleanSetting("drop_bowl", "Drop used soup bowl", true));
    private final BooleanSetting head = add(new BooleanSetting("head", "Use healing player heads", false));
    private final FloatSetting headHealth = add(new FloatSetting("head_health_percent", "Head health percent", 40, 1, 100));
    private final BooleanSetting headAbsorption = add(new BooleanSetting("head_absorption", "Count absorption for heads", true));
    private final FloatSetting headMaxAbsorption = add(new FloatSetting("head_max_absorption", "Maximum absorption for heads", 1, 0, 8));
    private final FloatSetting headCooldown = add(new FloatSetting("head_cooldown_seconds", "Head cooldown (seconds)", 0, 0, 120));
    private final BooleanSetting splash = add(new BooleanSetting("pot", "Throw beneficial splash potions", true));
    private final BooleanSetting splashHealing = add(new BooleanSetting("pot_health", "Splash Instant Health", true));
    private final BooleanSetting splashRegen = add(new BooleanSetting("pot_regen", "Splash Regeneration", true));
    private final BooleanSetting splashStrength = add(new BooleanSetting("pot_strength", "Splash Strength", true));
    private final BooleanSetting splashSpeed = add(new BooleanSetting("pot_speed", "Splash Speed", true));
    private final BooleanSetting splashFire = add(new BooleanSetting("pot_fire_resistance", "Splash Fire Resistance", true));
    private final BooleanSetting splashJump = add(new BooleanSetting("pot_jump_boost", "Splash Jump Boost", true));
    private final BooleanSetting splashWater = add(new BooleanSetting("pot_water_breathing", "Splash Water Breathing", true));
    private final FloatSetting splashHealth = add(new FloatSetting("pot_health_percent", "Splash healing health percent", 40, 1, 100));
    private final FloatSetting splashRegenHealth = add(new FloatSetting("pot_regen_health_percent", "Splash regen health percent", 40, 1, 100));
    private final FloatSetting groundDistance = add(new FloatSetting("ground_distance", "Maximum splash distance to ground", 2, 1, 5));
    private final BooleanSetting protectOthers = add(new BooleanSetting("do_not_benefit_others", "Avoid splashing nearby players", false));
    private final BooleanSetting allowLingering = add(new BooleanSetting("allow_lingering", "Use lingering potions", false));
    private final BooleanSetting drink = add(new BooleanSetting("drink", "Drink beneficial potions", false));
    private final BooleanSetting drinkHealing = add(new BooleanSetting("drink_health", "Drink Instant Health", true));
    private final BooleanSetting drinkRegen = add(new BooleanSetting("drink_regen", "Drink Regeneration", true));
    private final BooleanSetting drinkStrength = add(new BooleanSetting("drink_strength", "Drink Strength", true));
    private final BooleanSetting drinkSpeed = add(new BooleanSetting("drink_speed", "Drink Speed", true));
    private final BooleanSetting drinkFire = add(new BooleanSetting("drink_fire_resistance", "Drink Fire Resistance", true));
    private final BooleanSetting drinkJump = add(new BooleanSetting("drink_jump_boost", "Drink Jump Boost", true));
    private final BooleanSetting drinkWater = add(new BooleanSetting("drink_water_breathing", "Drink Water Breathing", true));
    private final FloatSetting drinkHealth = add(new FloatSetting("drink_health_percent", "Drink healing health percent", 40, 1, 100));
    private final FloatSetting drinkRegenHealth = add(new FloatSetting("drink_regen_health_percent", "Drink regen health percent", 40, 1, 100));
    private final BooleanSetting gapple = add(new BooleanSetting("gapple", "Eat golden apples", false));
    private final FloatSetting gappleHealth = add(new FloatSetting("gapple_health_percent", "Apple health percent", 40, 1, 100));
    private final BooleanSetting gappleAbsorption = add(new BooleanSetting("gapple_absorption", "Count absorption for apples", true));
    private final BooleanSetting enchantedGapple = add(new BooleanSetting("enchanted_gapple", "Allow enchanted golden apples", true));
    private final BooleanSetting autoSwap = add(new BooleanSetting("auto_swap", "Temporarily use main inventory items", false));
    private final BooleanSetting refill = add(new BooleanSetting("refill", "Refill empty hotbar slots", true));
    private final FloatSetting delayIn = add(new FloatSetting("delay_in_ticks", "Wait after inventory swap", 1, 0, 20));
    private final FloatSetting delayOut = add(new FloatSetting("delay_out_ticks", "Wait before restoring hotbar", 1, 0, 20));
    private final BooleanSetting avoidCombat = add(new BooleanSetting("not_during_combat", "Pause while taking or dealing hits", false));
    private final FloatSetting combatPause = add(new FloatSetting("combat_pause_ticks", "Pause combat after buff (ticks)", 2, 0, 40));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between buffs", 12, 2, 100));
    private LocalPlayer observedPlayer;
    private Candidate active;
    private InteractionHand hand;
    private Item usedItem;
    private ItemStack expected = ItemStack.EMPTY;
    private int usedCount;
    private int selectedSlot = -1;
    private int originalSlot = -1;
    private int inventorySwapSlot = -1;
    private int preparation;
    private int restoreDelay;
    private int age;
    private int cooldown;
    private int headWait;
    private int inventoryGuard;
    private boolean using;
    private boolean restoring;
    private boolean syntheticUse;
    private ClientLevel buffLevel;
    private long lastBuffTick = -1;

    public AutoBuff() {
        super("auto_buff", "Auto Buff", "Use soups, heads, apples, and beneficial potions when needed.", Category.PLAYER);
    }

    public boolean combatPaused(Minecraft client) {
        return enabled() && client.level != null && client.level == buffLevel && lastBuffTick >= 0
            && client.level.getGameTime() - lastBuffTick <= combatPause.get();
    }

    public boolean busy() { return enabled() && (active != null || inventoryGuard > 0); }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        if (observedPlayer != player) {
            reset(client);
            observedPlayer = player;
            cooldown = 20;
        }
        if (headWait > 0) headWait--;
        if (inventoryGuard > 0) inventoryGuard--;
        if (active != null) { advance(client); return; }
        if (cooldown > 0) { cooldown--; return; }
        if (client.gui.screen() != null || !player.isAlive() || player.isSpectator()
            || player.isUsingItem() || player.isHandsBusy() || SoupUse.busy(client)
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
            || AnomolyClient.MODULES.get(ThrowPot.class).active(client)
            || AnomolyClient.MODULES.get(Pot.class).active(client)
            || AnomolyClient.MODULES.get(AutoPotion.class).active(client)
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || avoidCombat.get() && (player.hurtTime > 0
                || AnomolyClient.MODULES.get(KillAura.class).hasTarget(client))) return;

        Candidate candidate = best(client, player);
        if (candidate == null) return;
        if (candidate.slot() >= 9 && candidate.slot() < 36 && refill.get()) {
            int empty = emptyHotbar(player);
            if (empty >= 0) {
                client.gameMode.handleContainerInput(player.inventoryMenu.containerId,
                    candidate.slot(), empty, ContainerInput.SWAP, player);
                cooldown = 2;
                inventoryGuard = 5;
                return;
            }
        }
        if (candidate.slot() >= 9 && candidate.slot() < 36 && !autoSwap.get()) return;
        begin(client, candidate);
    }

    private Candidate best(Minecraft client, LocalPlayer player) {
        Candidate best = null;
        for (int slot = 9; slot <= 45; slot++) {
            if (slot < 36 && !autoSwap.get() && (!refill.get() || emptyHotbar(player) < 0)) continue;
            ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
            Kind kind = kind(client, player, stack);
            if (kind == null) continue;
            int priority = priority(kind, stack);
            int preference = slot == 36 + player.getInventory().getSelectedSlot() ? 0
                : slot == 45 ? 1 : slot >= 36 ? 2 : 3;
            Candidate candidate = new Candidate(slot, kind, priority, preference);
            if (best == null || candidate.priority() < best.priority()
                || candidate.priority() == best.priority() && candidate.preference() < best.preference()) best = candidate;
        }
        return best;
    }

    private Kind kind(Minecraft client, LocalPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (splash.get() && (stack.is(Items.SPLASH_POTION) || allowLingering.get() && stack.is(Items.LINGERING_POTION))
            && potionNeeded(player, stack, false) && nearGround(client, player)
            && (!protectOthers.get() || !otherPlayerNearby(client, player))) return Kind.SPLASH;
        if (soup.get() && stack.is(Items.MUSHROOM_STEW)
            && lowHealth(player, soupHealth.get(), soupAbsorption.get())) return Kind.SOUP;
        if (head.get() && headWait == 0 && stack.is(Items.PLAYER_HEAD)
            && player.getAbsorptionAmount() <= headMaxAbsorption.get()
            && lowHealth(player, headHealth.get(), headAbsorption.get())) return Kind.HEAD;
        if (gapple.get() && (stack.is(Items.GOLDEN_APPLE) || enchantedGapple.get() && stack.is(Items.ENCHANTED_GOLDEN_APPLE))
            && lowHealth(player, gappleHealth.get(), gappleAbsorption.get())) return Kind.GAPPLE;
        if (drink.get() && stack.is(Items.POTION) && potionNeeded(player, stack, true)) return Kind.DRINK;
        return null;
    }

    private int priority(Kind kind, ItemStack stack) {
        return switch (kind) {
            case SPLASH -> hasEffect(stack, MobEffects.INSTANT_HEALTH) ? 0 : 4;
            case SOUP -> 1;
            case HEAD -> 2;
            case GAPPLE -> 3;
            case DRINK -> hasEffect(stack, MobEffects.INSTANT_HEALTH) ? 5 : 6;
        };
    }

    private boolean potionNeeded(LocalPlayer player, ItemStack stack, boolean drinking) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        boolean needed = false;
        for (var effect : contents.getAllEffects()) {
            var type = effect.getEffect();
            if (type.value().getCategory() == MobEffectCategory.HARMFUL) return false;
            if (type.equals(MobEffects.INSTANT_HEALTH))
                needed |= (drinking ? drinkHealing.get() : splashHealing.get())
                    && lowHealth(player, drinking ? drinkHealth.get() : splashHealth.get(), true);
            else if (type.equals(MobEffects.REGENERATION))
                needed |= (drinking ? drinkRegen.get() : splashRegen.get())
                    && lowHealth(player, drinking ? drinkRegenHealth.get() : splashRegenHealth.get(), true)
                    && !player.hasEffect(MobEffects.REGENERATION);
            else if (type.equals(MobEffects.STRENGTH))
                needed |= (drinking ? drinkStrength.get() : splashStrength.get()) && !player.hasEffect(type);
            else if (type.equals(MobEffects.SPEED))
                needed |= (drinking ? drinkSpeed.get() : splashSpeed.get()) && !player.hasEffect(type);
            else if (type.equals(MobEffects.FIRE_RESISTANCE))
                needed |= (drinking ? drinkFire.get() : splashFire.get()) && !player.hasEffect(type);
            else if (type.equals(MobEffects.JUMP_BOOST))
                needed |= (drinking ? drinkJump.get() : splashJump.get()) && !player.hasEffect(type);
            else if (type.equals(MobEffects.WATER_BREATHING))
                needed |= (drinking ? drinkWater.get() : splashWater.get()) && !player.hasEffect(type);
        }
        return needed;
    }

    private static boolean hasEffect(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        for (var effect : contents.getAllEffects()) if (effect.getEffect().equals(type)) return true;
        return false;
    }

    private static boolean lowHealth(LocalPlayer player, float percent, boolean absorption) {
        float current = player.getHealth() + (absorption ? player.getAbsorptionAmount() : 0);
        return current < player.getMaxHealth() && current * 100F <= player.getMaxHealth() * percent;
    }

    private boolean nearGround(Minecraft client, LocalPlayer player) {
        if (player.onGround()) return true;
        Vec3 feet = player.position();
        var hit = client.level.clip(new ClipContext(feet, feet.add(0, -groundDistance.get() - 0.5, 0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit instanceof BlockHitResult block && feet.y - block.getLocation().y <= groundDistance.get();
    }

    private static boolean otherPlayerNearby(Minecraft client, LocalPlayer player) {
        for (var entity : client.level.entitiesForRendering())
            if (entity instanceof Player other && other != player && other.isAlive()
                && player.canHarmPlayer(other) && player.distanceToSqr(other) <= 16) return true;
        return false;
    }

    private static int emptyHotbar(LocalPlayer player) {
        int selected = player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) if (i != selected && player.getInventory().getItem(i).isEmpty()) return i;
        return player.getInventory().getItem(selected).isEmpty() ? selected : -1;
    }

    private void begin(Minecraft client, Candidate candidate) {
        LocalPlayer player = client.player;
        active = candidate;
        expected = player.inventoryMenu.getSlot(candidate.slot()).getItem().copy();
        originalSlot = player.getInventory().getSelectedSlot();
        selectedSlot = originalSlot;
        hand = candidate.slot() == 45 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (candidate.slot() >= 9 && candidate.slot() < 36) {
            inventorySwapSlot = candidate.slot();
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId,
                inventorySwapSlot, selectedSlot, ContainerInput.SWAP, player);
            preparation = (int)delayIn.get();
        } else if (candidate.slot() >= 36 && candidate.slot() < 45) {
            selectedSlot = candidate.slot() - 36;
            player.getInventory().setSelectedSlot(selectedSlot);
        }
        if (preparation == 0) use(client);
    }

    private void advance(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player != observedPlayer || client.gameMode == null) { reset(client); return; }
        if (restoring) {
            if (restoreDelay > 0) { restoreDelay--; return; }
            restore(client);
            return;
        }
        if (client.gui.screen() != null || !player.isAlive() || player.isSpectator()
            || player.containerMenu != player.inventoryMenu
            || !using && player.isUsingItem()
            || selectedSlot >= 0 && hand == InteractionHand.MAIN_HAND
                && player.getInventory().getSelectedSlot() != selectedSlot) {
            finish(client, false);
            return;
        }
        if (preparation > 0) {
            preparation--;
            if (preparation == 0) use(client);
            return;
        }
        if (!using) return;
        age++;
        ItemStack current = player.getItemInHand(hand);
        if (!current.is(usedItem) || current.getCount() < usedCount) {
            finish(client, true);
            return;
        }
        if (player.isUsingItem() && player.getUsedItemHand() == hand && age <= 50) {
            client.options.keyUse.setDown(true);
            syntheticUse = true;
            return;
        }
        if (age >= 8) finish(client, false);
    }

    private void use(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) { reset(client); return; }
        ItemStack stack = player.getItemInHand(hand);
        if (expected.isEmpty() || !ItemStack.isSameItemSameComponents(expected, stack)) {
            finish(client, false);
            return;
        }
        usedItem = stack.getItem();
        usedCount = stack.getCount();
        age = 0;
        if (active.kind() == Kind.SPLASH) {
            float pitch = player.getXRot();
            player.setXRot(85F);
            InteractionResult result = client.gameMode.useItem(player, hand);
            player.setXRot(pitch);
            if (result.consumesAction()) {
                player.swing(hand, stack.getInteractAnimation(), false);
                player.itemUsed(hand);
            }
            finish(client, result.consumesAction());
            return;
        }
        client.gameMode.useItem(player, hand);
        using = true;
        if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            client.options.keyUse.setDown(true);
            syntheticUse = true;
        }
    }

    private void finish(Minecraft client, boolean success) {
        LocalPlayer player = client.player;
        if (using && player != null && client.gameMode != null && player.isUsingItem()
            && player.getUsedItemHand() == hand) client.gameMode.releaseUsingItem(player);
        finishTick(client);
        if (success && active != null && active.kind() == Kind.SOUP && dropBowl.get()
            && hand == InteractionHand.MAIN_HAND && player != null
            && player.getMainHandItem().is(Items.BOWL)
            && player.containerMenu == player.inventoryMenu && player.inventoryMenu.getCarried().isEmpty())
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId,
                36 + selectedSlot, 1, ContainerInput.THROW, player);
        if (success && active != null && active.kind() == Kind.HEAD) headWait = (int)(headCooldown.get() * 20);
        if (success && client.level != null) {
            buffLevel = client.level;
            lastBuffTick = client.level.getGameTime();
        }
        cooldown = success ? (int)interval.get() : 100;
        using = false;
        restoring = true;
        restoreDelay = (int)delayOut.get();
        if (restoreDelay == 0) restore(client);
    }

    private void restore(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && client.gameMode != null && inventorySwapSlot >= 0
            && player.containerMenu == player.inventoryMenu && player.inventoryMenu.getCarried().isEmpty())
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId,
                inventorySwapSlot, originalSlot, ContainerInput.SWAP, player);
        if (player != null && originalSlot >= 0 && hand == InteractionHand.MAIN_HAND
            && player.getInventory().getSelectedSlot() == selectedSlot)
            player.getInventory().setSelectedSlot(originalSlot);
        active = null;
        hand = null;
        usedItem = null;
        expected = ItemStack.EMPTY;
        selectedSlot = -1;
        originalSlot = -1;
        inventorySwapSlot = -1;
        preparation = 0;
        restoreDelay = 0;
        age = 0;
        restoring = false;
    }

    public void finishTick(Minecraft client) {
        if (syntheticUse) {
            client.options.keyUse.setDown(false);
            syntheticUse = false;
        }
    }

    public void disconnect() {
        reset(Minecraft.getInstance());
        observedPlayer = null;
    }

    private void reset(Minecraft client) {
        if (active != null && client.player == observedPlayer) {
            finish(client, false);
            if (active != null) restore(client);
        }
        active = null;
        observedPlayer = null;
        hand = null;
        expected = ItemStack.EMPTY;
        selectedSlot = -1;
        originalSlot = -1;
        inventorySwapSlot = -1;
        preparation = 0;
        restoreDelay = 0;
        age = 0;
        cooldown = 0;
        headWait = 0;
        inventoryGuard = 0;
        buffLevel = null;
        lastBuffTick = -1;
        using = false;
        restoring = false;
        finishTick(client);
    }

    @Override protected void onDisable(Minecraft client) { reset(client); }
}
