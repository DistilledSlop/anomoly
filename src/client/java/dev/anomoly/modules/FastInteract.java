package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.mixin.MinecraftAccessor;
import dev.anomoly.mixin.MultiPlayerGameModeAccessor;
import dev.anomoly.setting.BooleanSetting;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Separately limits the client's placement, mining, and discrete item-use delays. */
public final class FastInteract extends Module {
    private final BooleanSetting fastPlace = add(new BooleanSetting("fast_place", "Fast Place", true));
    private final FloatSetting placeInterval = add(new FloatSetting("place_interval", "Place interval (ticks)", 0, 0, 4));
    private final BooleanSetting fastBreak = add(new BooleanSetting("fast_break", "Fast Break", true));
    private final FloatSetting breakDelay = add(new FloatSetting("break_delay", "Break delay (ticks)", 0, 0, 5));
    private final BooleanSetting fastUse = add(new BooleanSetting("fast_use", "Fast Use", true));
    private final FloatSetting useInterval = add(new FloatSetting("use_interval", "Use interval (ticks)", 0, 0, 4));

    public FastInteract() {
        super("fast_interact", "Fast Interact",
            "Set separate client intervals for placing blocks, breaking blocks, and using items.", Category.PLAYER);
    }

    @Override public void tick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null) return;
        if (fastBreak.get() && client.gameMode != null) {
            MultiPlayerGameModeAccessor gameMode = (MultiPlayerGameModeAccessor) client.gameMode;
            gameMode.anomoly$setDestroyDelay(Math.min(gameMode.anomoly$getDestroyDelay(),
                Math.round(breakDelay.get())));
        }

        boolean placing = client.hitResult instanceof BlockHitResult hit
            && hit.getType() == HitResult.Type.BLOCK
            && (client.player.getMainHandItem().getItem() instanceof BlockItem
                || client.player.getOffhandItem().getItem() instanceof BlockItem);
        int interval;
        if (placing) {
            if (!fastPlace.get()) return;
            interval = Math.round(placeInterval.get());
        } else {
            if (!fastUse.get()) return;
            interval = Math.round(useInterval.get());
        }
        MinecraftAccessor accessor = (MinecraftAccessor) client;
        accessor.anomoly$setRightClickDelay(Math.min(accessor.anomoly$getRightClickDelay(), interval));
    }
}
