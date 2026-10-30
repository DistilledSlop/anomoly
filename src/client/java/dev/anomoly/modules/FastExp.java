package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.ChoiceSetting;
import dev.anomoly.setting.FloatSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Throws XP bottles from a held key, with a Mending-aware stop condition. */
public final class FastExp extends Module {
    private final BooleanSetting rotate = add(new BooleanSetting("rotate", "Aim bottles at your feet", true));
    private final BooleanSetting noWaste = add(new BooleanSetting("no_waste", "Only throw for damaged Mending gear", true));
    private final FloatSetting startDurability = add(new FloatSetting("min_durability_to_start_repair",
        "Start at remaining durability", 64, 0, 2048));
    private final FloatSetting continuePercent = add(new FloatSetting("max_durability_to_continue_repair",
        "Continue until durability percent", 85, 1, 100));
    private final ChoiceSetting mode = add(new ChoiceSetting("throw_mode", "Throw mode", "Normal", "Normal", "Fast"));
    private final FloatSetting normalMin = add(new FloatSetting("normal_min_ticks", "Normal minimum ticks per bottle", 2, 0.5F, 10));
    private final FloatSetting normalMax = add(new FloatSetting("normal_max_ticks", "Normal maximum ticks per bottle", 3, 0.5F, 10));
    private final FloatSetting fastMin = add(new FloatSetting("fast_min_items", "Fast minimum bottles per tick", 3, 0.5F, 16));
    private final FloatSetting fastMax = add(new FloatSetting("fast_max_items", "Fast maximum bottles per tick", 5, 0.5F, 16));
    private final FloatSetting combatPause = add(new FloatSetting("combat_pause_ticks", "Pause combat after throwing", 0, 0, 40));
    private final FloatSetting resetMin = add(new FloatSetting("slot_reset_min_ticks", "Minimum ticks before slot restore", 0, 0, 40));
    private final FloatSetting resetMax = add(new FloatSetting("slot_reset_max_ticks", "Maximum ticks before slot restore", 0, 0, 40));

    private LocalPlayer observedPlayer;
    private int originalSlot = -1;
    private int bottleSlot = -1;
    private int restoreDelay = -1;
    private int cooldown;
    private int pendingRepair;
    private int lastDeficit = -1;
    private float fastCarry;
    private boolean repairing;
    private ClientLevel lastLevel;
    private long lastThrowTick = -1;

    public FastExp() {
        super("fast_exp", "FastExp", "Hold its bound key to throw XP bottles at Mending gear.", Category.PLAYER);
    }

    public boolean busy(Minecraft client) {
        return enabled() && (AnomolyClient.fastExpHeld() || originalSlot >= 0 || restoreDelay >= 0);
    }

    public boolean combatPaused(Minecraft client) {
        return enabled() && combatPause.get() > 0 && client.level != null && client.level == lastLevel
            && lastThrowTick >= 0 && client.level.getGameTime() - lastThrowTick <= combatPause.get();
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        if (observedPlayer != player) {
            reset(client);
            observedPlayer = player;
        }
        if (originalSlot >= 0 && player.getInventory().getSelectedSlot() != bottleSlot) {
            originalSlot = -1;
            bottleSlot = -1;
            restoreDelay = -1;
        }
        if (!AnomolyClient.fastExpHeld() || client.gui.screen() != null || !player.isAlive()
            || player.isSpectator() || player.isUsingItem() || player.isHandsBusy()
            || client.options.keyUse.isDown() || client.options.keyAttack.isDown()
            || SoupUse.busy(client) || AnomolyClient.MODULES.get(AutoBuff.class).busy()
            || AnomolyClient.MODULES.get(SmartEat.class).busy()) {
            restoreLater(client);
            return;
        }
        restoreDelay = -1;
        InteractionHand hand = bottleHand(player);
        if (hand == null) {
            if (noWaste.get() && !hasRepairable(player, InteractionHand.MAIN_HAND)
                && !hasRepairable(player, InteractionHand.OFF_HAND)) repairing = false;
            restoreLater(client);
            return;
        }
        if (noWaste.get()) {
            if (!repairing) {
                if (!startNeeded(player, hand)) { restoreLater(client); return; }
                repairing = true;
                pendingRepair = 0;
                lastDeficit = -1;
            }
            int deficit = repairDeficit(player, hand);
            if (deficit <= 0) { repairing = false; restoreLater(client); return; }
            if (lastDeficit >= 0 && deficit < lastDeficit)
                pendingRepair = Math.max(0, pendingRepair - (lastDeficit - deficit));
            lastDeficit = deficit;
            if (client.level != null && lastThrowTick >= 0
                && client.level.getGameTime() - lastThrowTick > 20) pendingRepair = 0;
            if (pendingRepair >= deficit) { restoreLater(client); return; }
        }
        if (hand == InteractionHand.MAIN_HAND && !player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)) {
            int slot = findBottle(player);
            if (slot < 0) { restoreLater(client); return; }
            originalSlot = player.getInventory().getSelectedSlot();
            bottleSlot = slot;
            player.getInventory().setSelectedSlot(slot);
        }
        if (mode.get().equals("Normal")) {
            if (cooldown > 0) { cooldown--; return; }
            if (throwBottle(client, hand)) cooldown = Math.max(1,
                Math.round(random(normalMin.get(), normalMax.get())));
        } else {
            fastCarry += random(fastMin.get(), fastMax.get());
            int count = Math.min(16, (int)fastCarry);
            fastCarry -= count;
            for (int i = 0; i < count; i++) {
                if (noWaste.get() && pendingRepair >= lastDeficit) break;
                if (!throwBottle(client, hand)) break;
            }
        }
    }

    private InteractionHand bottleHand(LocalPlayer player) {
        if (player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE)
            && (!noWaste.get() || hasRepairable(player, InteractionHand.OFF_HAND))) return InteractionHand.OFF_HAND;
        if (player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)
            && (!noWaste.get() || hasRepairable(player, InteractionHand.MAIN_HAND))) return InteractionHand.MAIN_HAND;
        if (findBottle(player) >= 0
            && (!noWaste.get() || hasRepairable(player, InteractionHand.MAIN_HAND))) return InteractionHand.MAIN_HAND;
        return null;
    }

    private static int findBottle(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++)
            if (player.getInventory().getItem(slot).is(Items.EXPERIENCE_BOTTLE)) return slot;
        return -1;
    }

    private boolean startNeeded(LocalPlayer player, InteractionHand bottleHand) {
        for (ItemStack item : repairItems(player, bottleHand))
            if (mending(item) && item.getMaxDamage() - item.getDamageValue() <= startDurability.get()) return true;
        return false;
    }

    private boolean hasRepairable(LocalPlayer player, InteractionHand bottleHand) {
        for (ItemStack item : repairItems(player, bottleHand)) if (mending(item)) return true;
        return false;
    }

    private int repairDeficit(LocalPlayer player, InteractionHand bottleHand) {
        int deficit = 0;
        for (ItemStack item : repairItems(player, bottleHand)) {
            if (!mending(item)) continue;
            int targetDamage = (int)Math.floor(item.getMaxDamage() * (100F - continuePercent.get()) / 100F);
            deficit += Math.max(0, item.getDamageValue() - targetDamage);
        }
        return deficit;
    }

    private static ItemStack[] repairItems(LocalPlayer player, InteractionHand bottleHand) {
        return new ItemStack[] {
            player.getItemBySlot(EquipmentSlot.HEAD), player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS), player.getItemBySlot(EquipmentSlot.FEET),
            player.getItemInHand(bottleHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)
        };
    }

    private static boolean mending(ItemStack item) {
        if (!item.isDamageableItem() || item.getDamageValue() <= 0) return false;
        for (var enchantment : item.getEnchantments().entrySet())
            if (enchantment.getIntValue() > 0 && enchantment.getKey().unwrapKey()
                .map(key -> key.identifier().getPath().equals("mending")).orElse(false)) return true;
        return false;
    }

    private boolean throwBottle(Minecraft client, InteractionHand hand) {
        LocalPlayer player = client.player;
        ItemStack bottle = player.getItemInHand(hand);
        if (!bottle.is(Items.EXPERIENCE_BOTTLE) || player.getCooldowns().isOnCooldown(bottle)) return false;
        float pitch = player.getXRot();
        if (rotate.get()) player.setXRot(90F);
        InteractionResult result = client.gameMode.useItem(player, hand);
        if (rotate.get()) player.setXRot(pitch);
        if (!(result instanceof InteractionResult.Success)) return false;
        player.swing(hand, bottle.getInteractAnimation(), false);
        player.itemUsed(hand);
        if (noWaste.get()) pendingRepair += 14; // One bottle averages seven XP; Mending repairs two points per XP.
        if (client.level != null) { lastLevel = client.level; lastThrowTick = client.level.getGameTime(); }
        return true;
    }

    private void restoreLater(Minecraft client) {
        if (originalSlot < 0) return;
        if (restoreDelay < 0) restoreDelay = Math.round(random(resetMin.get(), resetMax.get()));
        if (restoreDelay > 0) { restoreDelay--; return; }
        if (client.player != null && client.player == observedPlayer
            && client.player.getInventory().getSelectedSlot() == bottleSlot)
            client.player.getInventory().setSelectedSlot(originalSlot);
        originalSlot = -1;
        bottleSlot = -1;
        restoreDelay = -1;
        cooldown = 0;
        fastCarry = 0;
    }

    private static float random(float a, float b) {
        float low = Math.min(a, b), high = Math.max(a, b);
        return low + ThreadLocalRandom.current().nextFloat() * (high - low);
    }

    private void reset(Minecraft client) {
        if (client.player != null && client.player == observedPlayer && originalSlot >= 0
            && client.player.getInventory().getSelectedSlot() == bottleSlot)
            client.player.getInventory().setSelectedSlot(originalSlot);
        observedPlayer = null;
        originalSlot = -1;
        bottleSlot = -1;
        restoreDelay = -1;
        cooldown = 0;
        pendingRepair = 0;
        lastDeficit = -1;
        fastCarry = 0;
        repairing = false;
        lastLevel = null;
        lastThrowTick = -1;
    }

    public void disconnect() { setEnabled(false); }

    @Override protected void onDisable(Minecraft client) { reset(client); }
}
