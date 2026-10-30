package dev.anomoly.modules;

import com.mojang.authlib.GameProfile;
import dev.anomoly.Module;
import dev.anomoly.setting.StringSetting;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;

public final class FakePlayer extends Module {
    private static final int ENTITY_ID = -73_100;
    private final StringSetting name = add(new StringSetting("name", "Name", "Anomoly"));
    private RemotePlayer fake;

    public FakePlayer() {
        super("fake_player", "Fake Player", "Spawn a local-only copy of yourself at your position as a stand-in body; other players cannot see it.", Category.PLAYER);
    }

    @Override protected void onEnable(Minecraft client) {
        if (client.level == null || client.player == null) return;
        fake = new RemotePlayer(client.level, new GameProfile(UUID.nameUUIDFromBytes(name.get().getBytes()), name.get()));
        fake.setId(ENTITY_ID);
        fake.snapTo(client.player.getX(), client.player.getY(), client.player.getZ(), client.player.getYRot(), client.player.getXRot());
        fake.setYHeadRot(client.player.getYHeadRot());
        client.level.addEntity(fake);
    }

    @Override protected void onDisable(Minecraft client) {
        if (client.level != null && fake != null) client.level.removeEntity(ENTITY_ID, Entity.RemovalReason.DISCARDED);
        fake = null;
    }
}
