package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.Items;

public final class Quiver extends Module {
    private int ticks;

    public Quiver() {
        super("quiver", "Quiver", "One-shot: look straight up, draw a hotbar bow fully and release, so the arrow falls back on you.", Category.COMBAT);
    }

    @Override protected void onEnable(Minecraft client) {
        ticks = 0;
        LocalPlayer player = client.player;
        int slot = -1;
        if (player != null) for (int i = 0; i < 9 && slot < 0; i++) if (player.getInventory().getItem(i).is(Items.BOW)) slot = i;
        if (slot < 0) {
            setEnabled(false);
            return;
        }
        player.getInventory().setSelectedSlot(slot);
        player.connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    @Override public void tick(Minecraft client) {
        client.player.setXRot(-90F);
        if (ticks++ < 22) {
            client.options.keyUse.setDown(true);
        } else {
            client.options.keyUse.setDown(false);
            setEnabled(false);
        }
    }

    @Override protected void onDisable(Minecraft client) { client.options.keyUse.setDown(false); }
}
