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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.food.FoodProperties;

/** Chooses food by hunger restored, with separate low-health emergency priorities. */
public final class SmartEat extends Module {
    private record Candidate(int slot, int priority, int nutrition, int preference) { }

    private final BooleanSetting autoEat = add(new BooleanSetting("auto_eat", "Eat automatically", true));
    private final FloatSetting minHunger = add(new FloatSetting("min_hunger", "Eat at or below hunger", 15, 0, 20));
    private final FloatSetting gappleHealth = add(new FloatSetting("prefer_gapple_health", "Golden apple at health", 9, 0, 20));
    private final FloatSetting notchHealth = add(new FloatSetting("prefer_notch_health", "Enchanted apple at health", 2, 0, 20));
    private final FloatSetting potionHealth = add(new FloatSetting("prefer_health_pot_health", "Healing potion at health", 12, 0, 20));
    private final BooleanSetting inventory = add(new BooleanSetting("inventory_food", "Use food from main inventory", true));
    private final FloatSetting swapBackDelay = add(new FloatSetting("swap_back_delay", "Ticks before restoring slot", 5, 1, 20));
    private final BooleanSetting avoidCombat = add(new BooleanSetting("not_during_combat", "Pause while fighting", false));
    private final FloatSetting combatPause = add(new FloatSetting("combat_pause_ticks", "Pause combat after eating", 0, 0, 40));

    private LocalPlayer observedPlayer;
    private Candidate active;
    private ItemStack expected = ItemStack.EMPTY;
    private Item usedItem;
    private InteractionHand hand;
    private int usedCount;
    private int originalSlot = -1;
    private int selectedSlot = -1;
    private int inventorySlot = -1;
    private int preparation;
    private int restoreDelay;
    private int age;
    private int cooldown;
    private boolean using;
    private boolean restoring;
    private boolean syntheticUse;
    private ClientLevel lastLevel;
    private long lastUseTick = -1;

    public SmartEat() {
        super("smart_eat", "SmartEat", "Choose and eat the best available food, with low-health priorities.", Category.PLAYER);
    }

    public boolean busy() { return enabled() && active != null; }

    public boolean combatPaused(Minecraft client) {
        return enabled() && combatPause.get() > 0 && client.level != null && client.level == lastLevel
            && lastUseTick >= 0 && client.level.getGameTime() - lastUseTick <= combatPause.get();
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        if (observedPlayer != player) {
            reset(client);
            observedPlayer = player;
            cooldown = 20;
        }
        if (active != null) { advance(client); return; }
        if (cooldown > 0) { cooldown--; return; }
        if (!autoEat.get() || client.gui.screen() != null || !player.isAlive() || player.isSpectator()
            || player.isUsingItem() || player.isHandsBusy() || SoupUse.busy(client)
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
            || AnomolyClient.MODULES.get(AutoBuff.class).busy()
            || AnomolyClient.MODULES.get(AutoBow.class).active(client)
            || AnomolyClient.MODULES.get(AutoShoot.class).active(client)
            || AnomolyClient.MODULES.get(AutoRod.class).active(client)
            || avoidCombat.get() && (player.hurtTime > 0
                || AnomolyClient.MODULES.get(KillAura.class).hasTarget(client))) return;
        Candidate candidate = best(player);
        if (candidate != null) begin(client, candidate);
    }

    private Candidate best(LocalPlayer player) {
        Candidate best = null;
        int hunger = player.getFoodData().getFoodLevel();
        boolean hungry = hunger <= (int)minHunger.get() && player.getFoodData().needsFood();
        for (int slot = 9; slot <= 45; slot++) {
            if (slot < 36 && !inventory.get()) continue;
            ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
            int priority = priority(player, stack, hungry);
            if (priority < 0) continue;
            FoodProperties food = stack.get(DataComponents.FOOD);
            int nutrition = food == null ? 0 : food.nutrition();
            int preference = slot == 36 + player.getInventory().getSelectedSlot() ? 0
                : slot == 45 ? 1 : slot >= 36 ? 2 : 3;
            Candidate candidate = new Candidate(slot, priority, nutrition, preference);
            if (best == null || candidate.priority() < best.priority()
                || candidate.priority() == best.priority() && candidate.nutrition() > best.nutrition()
                || candidate.priority() == best.priority() && candidate.nutrition() == best.nutrition()
                    && candidate.preference() < best.preference()) best = candidate;
        }
        return best;
    }

    private int priority(LocalPlayer player, ItemStack stack, boolean hungry) {
        if (stack.isEmpty()) return -1;
        float health = player.getHealth();
        float protectedHealth = health + player.getAbsorptionAmount();
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return protectedHealth <= notchHealth.get() ? 0 : -1;
        if (stack.is(Items.GOLDEN_APPLE)) return protectedHealth <= gappleHealth.get() ? 1 : -1;
        if (stack.is(Items.POTION) && health <= potionHealth.get()
            && isHealingPotion(stack)) return 2;
        if (!hungry || !stack.has(DataComponents.FOOD) || !stack.has(DataComponents.CONSUMABLE)
            || unsafeFood(stack)) return -1;
        return 3;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        boolean heals = false;
        for (var effect : contents.getAllEffects()) {
            if (effect.getEffect().equals(MobEffects.INSTANT_HEALTH)) heals = true;
            else if (effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL)
                return false;
        }
        return heals;
    }

    private static boolean unsafeFood(ItemStack stack) {
        // These foods can inflict harmful effects, or have unknown stew effects.
        return stack.is(Items.POISONOUS_POTATO) || stack.is(Items.PUFFERFISH)
            || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE)
            || stack.is(Items.CHICKEN) || stack.is(Items.SUSPICIOUS_STEW)
            || stack.is(Items.PLAYER_HEAD);
    }

    private void begin(Minecraft client, Candidate candidate) {
        LocalPlayer player = client.player;
        active = candidate;
        expected = player.inventoryMenu.getSlot(candidate.slot()).getItem().copy();
        originalSlot = player.getInventory().getSelectedSlot();
        selectedSlot = originalSlot;
        hand = candidate.slot() == 45 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (candidate.slot() >= 9 && candidate.slot() < 36) {
            inventorySlot = candidate.slot();
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId,
                inventorySlot, selectedSlot, ContainerInput.SWAP, player);
            preparation = 1;
        } else if (candidate.slot() < 45) {
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
            || hand == InteractionHand.MAIN_HAND && player.getInventory().getSelectedSlot() != selectedSlot
            || !using && player.isUsingItem()) { finish(client, false); return; }
        if (preparation > 0) {
            preparation--;
            if (preparation == 0) use(client);
            return;
        }
        if (!using) return;
        age++;
        ItemStack current = player.getItemInHand(hand);
        if (!current.is(usedItem) || current.getCount() < usedCount) { finish(client, true); return; }
        if (player.isUsingItem() && player.getUsedItemHand() == hand && age <= 80) {
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
        using = client.gameMode.useItem(player, hand).consumesAction();
        if (!using) { finish(client, false); return; }
        if (client.level != null) {
            lastLevel = client.level;
            lastUseTick = client.level.getGameTime();
        }
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
        using = false;
        cooldown = success ? 4 : 40;
        restoring = true;
        restoreDelay = (int)swapBackDelay.get();
    }

    private void restore(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && player == observedPlayer && client.gameMode != null && inventorySlot >= 0
            && player.containerMenu == player.inventoryMenu && player.inventoryMenu.getCarried().isEmpty())
            client.gameMode.handleContainerInput(player.inventoryMenu.containerId,
                inventorySlot, originalSlot, ContainerInput.SWAP, player);
        if (player != null && player == observedPlayer && originalSlot >= 0 && hand == InteractionHand.MAIN_HAND
            && player.getInventory().getSelectedSlot() == selectedSlot)
            player.getInventory().setSelectedSlot(originalSlot);
        active = null;
        expected = ItemStack.EMPTY;
        usedItem = null;
        hand = null;
        originalSlot = -1;
        selectedSlot = -1;
        inventorySlot = -1;
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
            if (using && client.gameMode != null && client.player.isUsingItem()
                && client.player.getUsedItemHand() == hand) client.gameMode.releaseUsingItem(client.player);
            restore(client);
        }
        active = null;
        observedPlayer = null;
        expected = ItemStack.EMPTY;
        usedItem = null;
        hand = null;
        originalSlot = -1;
        selectedSlot = -1;
        inventorySlot = -1;
        preparation = 0;
        restoreDelay = 0;
        age = 0;
        cooldown = 0;
        using = false;
        restoring = false;
        lastLevel = null;
        lastUseTick = -1;
        finishTick(client);
    }

    @Override protected void onDisable(Minecraft client) { reset(client); }
}
