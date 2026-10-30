package dev.anomoly.modules;

import dev.anomoly.AnomolyClient;
import dev.anomoly.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public final class MiddleClickFriend extends Module {
    private boolean wasDown;

    public MiddleClickFriend() {
        super("middle_click_friend", "Middle Click Friend", "Middle-click a player to add or remove them from the Teams friend list.", Category.MISC);
    }

    @Override public void tick(Minecraft client) {
        boolean down = client.mouseHandler.isMiddlePressed();
        boolean click = down && !wasDown;
        wasDown = down;
        if (!click || client.gui.screen() != null || !(client.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Player other)) return;
        Teams teams = AnomolyClient.MODULES.get(Teams.class);
        teams.toggle(other.getName().getString());
        client.player.sendSystemMessage(Component.literal("[Anomoly] " + other.getName().getString()
            + (teams.listed(other.getName().getString()) ? " added to" : " removed from") + " friends."));
    }
}
