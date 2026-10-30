package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One use attempt shared by automatic and manually triggered soup modules. */
final class SoupUse {
    private LocalPlayer player;
    private InteractionHand hand;
    private Item item;
    private int originalSlot = -1;
    private int soupSlot = -1;
    private int age;
    private boolean restoreSlot;
    private boolean syntheticUse;
    private boolean consumed;

    static boolean busy(Minecraft client) {
        return AnomolyClient.MODULES.get(AutoSoup.class).using(client)
            || AnomolyClient.MODULES.get(Soup.class).using(client);
    }

    boolean using(Minecraft client) { return player != null && player == client.player; }
    boolean active() { return player != null; }
    boolean consumed() { return consumed; }

    boolean start(Minecraft client, boolean selectHotbar, boolean otherStews, boolean restoreSlot) {
        if (player != null && player != client.player) cancel(client);
        if (client.player == null || client.gameMode == null || using(client)) return false;
        LocalPlayer current = client.player;
        InteractionHand selectedHand = null;
        int slot = current.getInventory().getSelectedSlot();
        if (isSoup(current.getMainHandItem(), otherStews)) selectedHand = InteractionHand.MAIN_HAND;
        else if (isSoup(current.getOffhandItem(), otherStews)) selectedHand = InteractionHand.OFF_HAND;
        else if (selectHotbar) for (int i = 0; i < 9; i++) {
            if (!isSoup(current.getInventory().getItem(i), otherStews)) continue;
            selectedHand = InteractionHand.MAIN_HAND;
            slot = i;
            break;
        }
        if (selectedHand == null) return false;
        player = current;
        hand = selectedHand;
        originalSlot = current.getInventory().getSelectedSlot();
        soupSlot = selectedHand == InteractionHand.MAIN_HAND ? slot : -1;
        this.restoreSlot = restoreSlot;
        age = 0;
        consumed = false;
        if (soupSlot >= 0) current.getInventory().setSelectedSlot(soupSlot);
        ItemStack stack = current.getItemInHand(hand);
        item = stack.getItem();
        // Send a normal use packet even when vanilla rejects food at full hunger: soup
        // PvP servers may consume and heal on that interaction.
        client.gameMode.useItem(current, hand);
        holdUse(client);
        return true;
    }

    /** Returns true when the attempt has ended, whether consumed or rejected. */
    boolean tick(Minecraft client) {
        if (player == null) return true;
        if (player != client.player || client.gameMode == null || client.gui.screen() != null
            || player.isSpectator() || !player.isAlive()
            || soupSlot >= 0 && player.getInventory().getSelectedSlot() != soupSlot) {
            cancel(client);
            return true;
        }
        age++;
        if (!player.getItemInHand(hand).is(item)) {
            consumed = true;
            finish(client, true);
            return true;
        }
        if (player.isUsingItem() && player.getUsedItemHand() == hand) {
            if (age > 50) { cancel(client); return true; }
            holdUse(client);
            return false;
        }
        // A plugin can change the stack after the use packet. Give it a few ticks
        // before treating a failed vanilla use as an unsuccessful attempt.
        if (age >= 8) { finish(client, false); return true; }
        return false;
    }

    void finishTick(Minecraft client) {
        if (syntheticUse) {
            client.options.keyUse.setDown(false);
            syntheticUse = false;
        }
    }

    void cancel(Minecraft client) { finish(client, true); }

    private void finish(Minecraft client, boolean release) {
        if (release && player != null && player == client.player && client.gameMode != null && player.isUsingItem()
            && player.getUsedItemHand() == hand) client.gameMode.releaseUsingItem(player);
        finishTick(client);
        if (restoreSlot && player != null && player == client.player && soupSlot >= 0
            && player.getInventory().getSelectedSlot() == soupSlot)
            player.getInventory().setSelectedSlot(originalSlot);
        player = null;
        hand = null;
        item = null;
        originalSlot = -1;
        soupSlot = -1;
        age = 0;
    }

    private void holdUse(Minecraft client) {
        if (player.isUsingItem() && player.getUsedItemHand() == hand && !client.options.keyUse.isDown()) {
            client.options.keyUse.setDown(true);
            syntheticUse = true;
        }
    }

    private static boolean isSoup(ItemStack stack, boolean otherStews) {
        return stack.is(Items.MUSHROOM_STEW) || otherStews
            && (stack.is(Items.RABBIT_STEW) || stack.is(Items.BEETROOT_SOUP));
    }
}
