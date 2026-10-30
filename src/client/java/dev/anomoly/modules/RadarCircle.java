package dev.anomoly.modules;

import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

public final class RadarCircle extends Module implements HudDrawable {
    public RadarCircle() {
        super("radar_circle", "RadarRewrite", "Draw a circular radar showing nearby players relative to your facing.", Category.RENDER);
    }

    @Override public void draw(Minecraft client, GuiGraphicsExtractor graphics) {
        int cx = 50, cy = graphics.guiHeight() - 120, r = 40;
        for (int angle = 0; angle < 360; angle += 6) {
            int x = cx + (int)(Math.cos(Math.toRadians(angle)) * r);
            int y = cy + (int)(Math.sin(Math.toRadians(angle)) * r);
            graphics.fill(x, y, x + 1, y + 1, 0xFF55DDBB);
        }
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        double yaw = Math.toRadians(client.player.getYRot());
        for (Player other : client.level.players()) {
            if (other == client.player) continue;
            double dx = other.getX() - client.player.getX(), dz = other.getZ() - client.player.getZ();
            double rx = dx * Math.cos(yaw) + dz * Math.sin(yaw);
            double rz = dz * Math.cos(yaw) - dx * Math.sin(yaw);
            double distance = Math.max(Math.hypot(dx, dz), 1.0E-3);
            double norm = Math.min(distance, 64.0) / 64.0 * r;
            int x = cx + (int)(rx / distance * norm);
            int y = cy - (int)(rz / distance * norm);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFF5555);
        }
    }
}
