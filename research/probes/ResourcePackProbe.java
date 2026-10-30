package com.example;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;

final class ResourcePackProbe {
    static void setup(MinecraftServer server) {
        try {
            var info = server.getServerResourcePack().orElseThrow();
            var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pack-probe"), false);
            var connection = new Connection(PacketFlow.SERVERBOUND);
            var channel = new EmbeddedChannel(connection);
            var listener = new ServerConfigurationPacketListenerImpl(server, connection, cookie);
            var task = new ServerResourcePackConfigurationTask(info);
            Field field = ServerConfigurationPacketListenerImpl.class.getDeclaredField("currentTask");
            field.setAccessible(true);
            field.set(listener, task);
            UUID wrongId = UUID.randomUUID();
            listener.handleResourcePackResponse(new ServerboundResourcePackPacket(wrongId, ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD));
            ExampleMod.LOGGER.info("PACK_PROBE required={} offered={} response={} action=FAILED_DOWNLOAD taskFinished={} connected={}",
                server.isResourcePackRequired(), info.id(), wrongId, field.get(listener) == null, connection.isConnected());
            channel.finishAndReleaseAll();
        } catch (Throwable e) {
            ExampleMod.LOGGER.error("PACK_PROBE failed", e);
        }
    }
}
