package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class CrosshairArrows extends Module implements HudDrawable {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 64, 8, 256));

    public CrosshairArrows() {
        super("crosshair_arrows", "CrosshairArrows", "Draw markers on a ring around the crosshair that point toward nearby players.", Category.RENDER);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        for (Player other : client.level.players()) {
            if (other == client.player || client.player.distanceToSqr(other) > range.get() * range.get()) continue;
            double bearing = Math.toDegrees(Math.atan2(-(other.getX() - client.player.getX()), other.getZ() - client.player.getZ()));
            double relative = Math.toRadians(Mth.wrapDegrees((float)(bearing - client.player.getYRot())));
            int x = cx + (int)(Math.sin(relative) * 40);
            int y = cy - (int)(Math.cos(relative) * 40);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFFF5555);
        }
    }
}
