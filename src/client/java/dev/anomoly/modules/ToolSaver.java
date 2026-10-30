package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.IntSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;

public final class ToolSaver extends Module {
    private final IntSetting remaining = add(new IntSetting("remaining", "Switch away at remaining durability", 5, 1, 50));

    public ToolSaver() {
        super("tool_saver", "ToolSaver", "Move off a held tool when its durability is nearly gone (also covers Item Saver).", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (!low(player.getMainHandItem())) return;
        int current = player.getInventory().getSelectedSlot();
        for (int offset = 1; offset < 9; offset++) {
            int slot = (current + offset) % 9;
            ItemStack stack = player.getInventory().getItem(slot);
            if (!low(stack) && !stack.isDamageableItem()) {
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ServerboundSetCarriedItemPacket(slot));
                return;
            }
        }
    }

    private boolean low(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= remaining.get();
    }
}
