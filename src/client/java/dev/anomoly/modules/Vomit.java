package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public final class Vomit extends Module {
    private final IntSetting interval = add(new IntSetting("interval_ticks", "Ticks between drops", 4, 1, 40));
    private final Random random = new Random();
    private int ticks;

    public Vomit() {
        super("vomit", "Vomit", "Keep throwing random hotbar stacks out of your inventory.", Category.FUN);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || ++ticks % interval.get() != 0) return;
        var player = client.player;
        int start = random.nextInt(9);
        for (int i = 0; i < 9; i++) {
            int slot = (start + i) % 9;
            if (player.getInventory().getItem(slot).isEmpty()) continue;
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS,
                net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN));
            player.getInventory().setItem(slot, net.minecraft.world.item.ItemStack.EMPTY);
            return;
        }
    }
}
