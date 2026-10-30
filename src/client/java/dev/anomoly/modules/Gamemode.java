package dev.anomoly.modules;

import dev.anomoly.Module;
import dev.anomoly.setting.ChoiceSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;

public final class Gamemode extends Module {
    private final ChoiceSetting mode = add(new ChoiceSetting("mode", "Local game mode", "Creative", "Creative", "Spectator", "Adventure", "Survival"));
    private GameType original;

    public Gamemode() {
        super("gamemode", "Gamemode", "Switch only the client's local game mode so creative or spectator camera behavior applies; the server still enforces its own rules.", Category.WORLD);
    }

    @Override protected void onEnable(Minecraft client) {
        if (client.gameMode == null) return;
        original = client.gameMode.getPlayerMode();
        client.gameMode.setLocalMode(switch (mode.get()) {
            case "Spectator" -> GameType.SPECTATOR;
            case "Adventure" -> GameType.ADVENTURE;
            case "Survival" -> GameType.SURVIVAL;
            default -> GameType.CREATIVE;
        });
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.gameMode != null && original != null) client.gameMode.setLocalMode(original);
    }
}
