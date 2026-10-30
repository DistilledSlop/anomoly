package com.example;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

final class LocatorPulseProbe {
	private static ServerPlayer source;
	private static ServerPlayer receiver;
	private static EmbeddedChannel receiverChannel;
	private static int ticks;

	static void setup(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			receiver = login(server, level, "waypoint-receiver", true);
			source = login(server, level, "waypoint-source", false);
			receiver.absSnapTo(0.5, 100, 0.5, 0, 0);
			source.absSnapTo(400.5, 100, 400.5, 0, 0);
			receiver.connection.resetPosition();
			source.connection.resetPosition();
			source.connection.tick();
			ExampleMod.LOGGER.info("LOCATOR_PULSE ready receiver={} source={} source_transmits={} receiver_receives={}", receiver.position(), source.position(), source.isTransmittingWaypoint(), receiver.isReceivingWaypoints());
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("LOCATOR_PULSE setup failed", e);
		}
	}

	private static ServerPlayer login(MinecraftServer server, ServerLevel level, String name, boolean capture) throws Exception {
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
		ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		EmbeddedChannel channel = new EmbeddedChannel(connection);
		if (capture) receiverChannel = channel;
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		Field f = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
		f.setAccessible(true);
		int id = f.getInt(player.connection);
		var pos = player.position();
		player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id, pos.x, pos.y, pos.z, 0, 0));
		return player;
	}

	static void tick(MinecraftServer server) {
		if (receiver == null || source == null) return;
		try {
			if (++ticks == 5) {
				source.makeWaypointConnectionWith(receiver).ifPresent(connection -> connection.connect());
				drain("initial");
				source.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(408.5, 100, 400.5, true, false));
				drain("excursion");
				source.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				source.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(400.5, 100, 400.5, true, false));
				drain("return");
			}
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("LOCATOR_PULSE tick failed", e);
			receiver = null;
		}
	}

	private static void drain(String label) throws Exception {
		receiverChannel.runPendingTasks();
		Object message;
		int count = 0;
		while ((message = receiverChannel.readOutbound()) != null) {
			count++;
			if (message instanceof ClientboundTrackedWaypointPacket packet) {
				Field f = packet.waypoint().getClass().getDeclaredField("angle");
				f.setAccessible(true);
				ExampleMod.LOGGER.info("LOCATOR_PULSE {} receiver={} uuid={} angle={} operation={} type={}", label, receiver.position(), packet.waypoint().id(), f.getFloat(packet.waypoint()), packet.operation(), packet.waypoint().getClass().getSimpleName());
			}
		}
		ExampleMod.LOGGER.info("LOCATOR_PULSE {} drained={}", label, count);
	}
}
