package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Sends a physical right-click to the held usable item rather than an interactable block. */
public final class NoBlockInteract extends Module {
    public NoBlockInteract() {
        super("no_block_interact", "NoBlockInteract",
            "Use a held item while aiming at a container, door, or other interactive block.", Category.PLAYER);
    }

    public boolean bypass(Minecraft client) {
        if (!enabled() || client.player == null || client.level == null || client.gui.screen() != null
            || client.player.isSpectator() || client.player.isSecondaryUseActive()
            || !(client.hitResult instanceof BlockHitResult hit)
            || hit.getType() != HitResult.Type.BLOCK) return false;
        var state = client.level.getBlockState(hit.getBlockPos());
        var block = state.getBlock();
        boolean interactive = state.getMenuProvider(client.level, hit.getBlockPos()) != null
            || block instanceof DoorBlock || block instanceof TrapDoorBlock
            || block instanceof FenceGateBlock || block instanceof ButtonBlock
            || block instanceof LeverBlock || block instanceof BedBlock
            || block instanceof NoteBlock || block instanceof BellBlock
            || block instanceof CakeBlock || block instanceof ComposterBlock
            || block instanceof JukeboxBlock || block instanceof RespawnAnchorBlock;
        return interactive && (usable(client.player.getMainHandItem(), client)
            || usable(client.player.getOffhandItem(), client));
    }

    private static boolean usable(ItemStack stack, Minecraft client) {
        return !stack.isEmpty() && (stack.has(DataComponents.CONSUMABLE)
            || stack.getUseDuration(client.player) > 0
            || stack.is(Items.ENDER_PEARL) || stack.is(Items.EGG)
            || stack.is(Items.SNOWBALL) || stack.is(Items.EXPERIENCE_BOTTLE)
            || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)
            || stack.is(Items.FIREWORK_ROCKET) || stack.is(Items.WIND_CHARGE)
            || stack.is(Items.FISHING_ROD));
    }
}
