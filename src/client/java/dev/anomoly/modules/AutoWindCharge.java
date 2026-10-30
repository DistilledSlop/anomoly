package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class AutoWindCharge extends Module {
    public AutoWindCharge() {
        super("auto_wind_charge", "AutoWindCharge", "One-shot: throw a hotbar wind charge straight down at your feet to launch yourself, then switch off.", Category.PLAYER);
    }

    @Override protected void onEnable(Minecraft client) {
        try {
            var player = client.player;
            if (player == null || client.gameMode == null) return;
            int slot = -1;
            for (int i = 0; i < 9 && slot < 0; i++) if (player.getInventory().getItem(i).is(Items.WIND_CHARGE)) slot = i;
            if (slot < 0) return;
            int previous = player.getInventory().getSelectedSlot();
            float pitch = player.getXRot();
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            player.setXRot(90F);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.setXRot(pitch);
            player.getInventory().setSelectedSlot(previous);
            player.connection.send(new ServerboundSetCarriedItemPacket(previous));
        } finally {
            setEnabled(false);
        }
    }
}
