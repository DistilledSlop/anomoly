package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AutoTool extends Module {
    private final BooleanSetting restore = add(new BooleanSetting("restore", "Restore held item", true));
    private int originalSlot = -1;
    private int toolSlot = -1;

    public AutoTool() {
        super("auto_tool", "Auto Tool", "Select the fastest suitable hotbar tool while mining.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (client.gui.screen() != null || !client.options.keyAttack.isDown()
            || !(client.hitResult instanceof BlockHitResult hit)) {
            finish(client);
            return;
        }
        var inventory = client.player.getInventory();
        BlockState state = client.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) { finish(client); return; }
        int selected = inventory.getSelectedSlot();
        if (originalSlot < 0) originalSlot = selected;
        else if (toolSlot >= 0 && selected != toolSlot) originalSlot = selected;
        int best = selected;
        float bestSpeed = inventory.getItem(selected).getDestroySpeed(state);
        boolean bestDrops = inventory.getItem(selected).isCorrectToolForDrops(state);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            boolean drops = stack.isCorrectToolForDrops(state);
            float speed = stack.getDestroySpeed(state);
            if ((drops && !bestDrops) || (drops == bestDrops && speed > bestSpeed + 0.01F)) {
                best = slot;
                bestSpeed = speed;
                bestDrops = drops;
            }
        }
        if (best != selected) inventory.setSelectedSlot(best);
        toolSlot = best;
    }

    private void finish(Minecraft client) {
        if (originalSlot >= 0 && restore.get() && client.player != null) {
            var inventory = client.player.getInventory();
            if (inventory.getSelectedSlot() == toolSlot) inventory.setSelectedSlot(originalSlot);
        }
        originalSlot = -1;
        toolSlot = -1;
    }

    @Override protected void onDisable(Minecraft client) { finish(client); }
}
