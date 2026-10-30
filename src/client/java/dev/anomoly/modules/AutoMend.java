package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import dev.anomoly.setting.StringSetting;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Rotates damaged Mending items into the offhand for collected experience to repair. */
public final class AutoMend extends Module {
    private final StringSetting blacklist = add(new StringSetting("blacklist", "Excluded item IDs", ""));
    private final BooleanSetting force = add(new BooleanSetting("force", "Replace other offhand items", false));
    private final BooleanSetting autoDisable = add(new BooleanSetting("auto_disable", "Turn off when all items are repaired", true));
    private final FloatSetting interval = add(new FloatSetting("interval_ticks", "Ticks between swaps", 4F, 1F, 20F));
    private LocalPlayer observedPlayer;
    private int startupDelay;
    private int cooldown;
    private boolean placedOffhand;
    private int displacedOffhandSlot = -1;
    private ItemStack displacedOffhand = ItemStack.EMPTY;

    public AutoMend() {
        super("auto_mend", "Auto Mend", "Rotate damaged Mending items through your offhand as they are repaired.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        if (observedPlayer != player) {
            observedPlayer = player;
            startupDelay = 20;
            cooldown = 0;
            placedOffhand = false;
            displacedOffhandSlot = -1;
            displacedOffhand = ItemStack.EMPTY;
            return;
        }
        if (startupDelay > 0) { startupDelay--; return; }
        if (cooldown > 0) { cooldown--; return; }
        if (client.gui.screen() != null || player.isSpectator() || player.isUsingItem()
            || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()
            || AnomolyClient.MODULES.get(AutoTotem.class).enabled()) return;

        ItemStack offhand = player.getOffhandItem();
        if (mending(offhand) && offhand.getDamageValue() > 0) return;
        if (!offhand.isEmpty() && !mending(offhand) && !force.get()) return;

        var menu = player.inventoryMenu;
        Set<String> excluded = StorageMenu.ids(blacklist.get());
        int candidate = -1;
        for (int slot = 9; slot <= 44; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!mending(stack) || stack.getDamageValue() <= 0 || excluded.contains(StorageMenu.id(stack))) continue;
            candidate = slot;
            break;
        }
        if (candidate >= 0) {
            if (displacedOffhandSlot < 0 && !offhand.isEmpty() && !mending(offhand)) {
                displacedOffhandSlot = candidate;
                displacedOffhand = offhand.copy();
            }
            swap(client, candidate, !offhand.isEmpty());
            placedOffhand = true;
            cooldown = (int) interval.get();
            return;
        }

        if (placedOffhand && mending(offhand) && offhand.getDamageValue() == 0) {
            if (displacedOffhandSlot >= 0 && ItemStack.isSameItemSameComponents(
                displacedOffhand, menu.getSlot(displacedOffhandSlot).getItem())) {
                swap(client, displacedOffhandSlot, true);
                placedOffhand = false;
                cooldown = (int) interval.get();
            } else {
                int empty = -1;
                for (int slot = 9; slot <= 44; slot++)
                    if (menu.getSlot(slot).getItem().isEmpty()) { empty = slot; break; }
                if (empty >= 0) {
                    int id = menu.containerId;
                    client.gameMode.handleContainerInput(id, 45, 0, ContainerInput.PICKUP, player);
                    client.gameMode.handleContainerInput(id, empty, 0, ContainerInput.PICKUP, player);
                    placedOffhand = false;
                    cooldown = (int) interval.get();
                }
            }
        }
        if (autoDisable.get()) setEnabled(false);
    }

    private void swap(Minecraft client, int source, boolean occupied) {
        LocalPlayer player = client.player;
        int id = player.inventoryMenu.containerId;
        client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(id, 45, 0, ContainerInput.PICKUP, player);
        if (occupied) client.gameMode.handleContainerInput(id, source, 0, ContainerInput.PICKUP, player);
    }

    private static boolean mending(ItemStack stack) {
        if (!stack.isDamageableItem()) return false;
        for (var entry : stack.getEnchantments().entrySet())
            if (entry.getIntValue() > 0 && entry.getKey().unwrapKey()
                .map(key -> key.identifier().getPath().equals("mending")).orElse(false)) return true;
        return false;
    }

    @Override protected void onDisable(Minecraft client) {
        observedPlayer = null;
        startupDelay = 0;
        cooldown = 0;
        placedOffhand = false;
        displacedOffhandSlot = -1;
        displacedOffhand = ItemStack.EMPTY;
    }
}
