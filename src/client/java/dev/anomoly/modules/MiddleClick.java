package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class MiddleClick extends Module {
    private final ChoiceSetting item = add(new ChoiceSetting("item", "Use on middle click", "Ender pearl", "Ender pearl", "Firework rocket", "Wind charge"));
    private boolean wasDown;

    public MiddleClick() {
        super("middle_click", "MiddleClick", "Use a hotbar ender pearl, rocket or wind charge when you press the middle mouse button (also covers MiddleClickAction).", Category.MISC);
    }

    @Override public void tick(Minecraft client) {
        boolean down = client.mouseHandler.isMiddlePressed();
        boolean click = down && !wasDown;
        wasDown = down;
        if (!click || client.gui.screen() != null || client.gameMode == null) return;
        LocalPlayer player = client.player;
        Item wanted = switch (item.get()) {
            case "Firework rocket" -> Items.FIREWORK_ROCKET;
            case "Wind charge" -> Items.WIND_CHARGE;
            default -> Items.ENDER_PEARL;
        };
        int slot = -1;
        for (int i = 0; i < 9 && slot < 0; i++) if (player.getInventory().getItem(i).is(wanted)) slot = i;
        if (slot < 0) return;
        int previous = player.getInventory().getSelectedSlot();
        if (slot != previous) {
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        if (slot != previous) {
            player.getInventory().setSelectedSlot(previous);
            player.connection.send(new ServerboundSetCarriedItemPacket(previous));
        }
    }
}
