package com.example;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class CrafterProbe {
	private static ServerPlayer player;
	private static CrafterBlockEntity crafter;
	private static int ticks;
	private static final int Y = 100;

	static void setup(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "crafter-probe"), false);
			player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
			Connection connection = new Connection(PacketFlow.SERVERBOUND);
			new EmbeddedChannel(connection);
			server.getPlayerList().placeNewPlayer(connection, player, cookie);
			player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
			Field f = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
			f.setAccessible(true);
			int id = f.getInt(player.connection);
			Vec3 pos = player.position();
			player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id, pos.x, pos.y, pos.z, 0, 0));
			player.absSnapTo(0.5, Y, 0.5, 0, 0);
			player.connection.resetPosition();
			level.getChunkSource().move(player);
			for (int x = -3; x <= 12; x++) {
				for (int y = Y; y <= Y + 3; y++) level.setBlockAndUpdate(new BlockPos(x, y, 0), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(x, Y - 1, 0), Blocks.STONE.defaultBlockState());
			}
			BlockPos crafterPos = new BlockPos(-2, Y, 0);
			level.setBlockAndUpdate(crafterPos, Blocks.CRAFTER.defaultBlockState());
			crafter = (CrafterBlockEntity)level.getBlockEntity(crafterPos);
			ExampleMod.LOGGER.info("CRAFTER_PROBE ready player={} crafter={} disabled={}", player.position(), crafterPos, crafter.isSlotDisabled(0));
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("CRAFTER_PROBE setup failed", e);
		}
	}

	static void tick(MinecraftServer server) {
		if (player == null || crafter == null) return;
		try {
			if (++ticks == 5) {
				BlockPos pos = crafter.getBlockPos();
				player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false), 1));
				ExampleMod.LOGGER.info("CRAFTER_PROBE opened={} at={}", player.containerMenu.getClass().getSimpleName(), player.position());
				if (!(player.containerMenu instanceof CrafterMenu menu)) throw new IllegalStateException("Crafter menu did not open");
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(8.5, Y, 0.5, true, false));
				ExampleMod.LOGGER.info("CRAFTER_PROBE remote_pos={} valid={} disabled_before={}", player.position(), menu.stillValid(player), crafter.isSlotDisabled(0));
				player.connection.handleContainerSlotStateChanged(new ServerboundContainerSlotStateChangedPacket(0, menu.containerId, false));
				ExampleMod.LOGGER.info("CRAFTER_PROBE disabled_after={} menu={}", crafter.isSlotDisabled(0), player.containerMenu.getClass().getSimpleName());
			} else if (ticks == 6) {
				player.connection.tick();
				ExampleMod.LOGGER.info("CRAFTER_PROBE next_tick_menu={} disabled={}", player.containerMenu.getClass().getSimpleName(), crafter.isSlotDisabled(0));
			}
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("CRAFTER_PROBE tick failed", e);
			player = null;
		}
	}
}
