package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.FloatSetting;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

public final class PlayerRadar extends Module {
    private final FloatSetting range = add(new FloatSetting("range", "Range", 128, 16, 512));
    private final FloatSetting maxPlayers = add(new FloatSetting("max_players", "Maximum players", 8, 1, 16));

    public PlayerRadar() {
        super("player_radar", "Player Radar", "Show nearby loaded players, relative direction, and distance on the HUD and ClickGUI.", Category.RENDER);
    }

    public void drawHud(Minecraft client, GuiGraphicsExtractor graphics) {
        if (!enabled() || client.player == null || client.level == null) return;
        double rangeSquared = range.get() * range.get();
        List<AbstractClientPlayer> nearby = client.level.players().stream()
            .filter(player -> player != client.player && client.player.distanceToSqr(player) <= rangeSquared)
            .sorted(Comparator.comparingDouble(client.player::distanceToSqr))
            .limit((int)maxPlayers.get())
            .toList();
        if (nearby.isEmpty()) return;

        int panelWidth = 174;
        int x = graphics.guiWidth() - panelWidth - 8;
        int y = 8;
        int panelHeight = 18 + nearby.size() * 12;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xB0101420);
        graphics.fill(x, y, x + 2, y + panelHeight, 0xFF55DDBB);
        graphics.text(client.font, "RADAR  " + nearby.size(), x + 8, y + 5, 0xFF66EACC);

        for (int i = 0; i < nearby.size(); i++) {
            AbstractClientPlayer player = nearby.get(i);
            double dx = player.getX() - client.player.getX();
            double dz = player.getZ() - client.player.getZ();
            double bearing = Math.toDegrees(Math.atan2(-dx, dz));
            float relative = Mth.wrapDegrees((float)(bearing - client.player.getYRot()));
            String name = player.getName().getString();
            if (name.length() > 15) name = name.substring(0, 14) + "…";
            String row = String.format(Locale.ROOT, "%s  %s  %.0fm", direction(relative), name, client.player.distanceTo(player));
            graphics.text(client.font, row, x + 8, y + 18 + i * 12, 0xFFE4F6F3);
        }
    }

    private static String direction(float angle) {
        int sector = Math.floorMod(Math.round(angle / 45F), 8);
        return switch (sector) {
            case 0 -> "↑";
            case 1 -> "↗";
            case 2 -> "→";
            case 3 -> "↘";
            case 4 -> "↓";
            case 5 -> "↙";
            case 6 -> "←";
            default -> "↖";
        };
    }
}
