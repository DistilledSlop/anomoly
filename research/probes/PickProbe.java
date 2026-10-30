package com.example;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

final class PickProbe {
	private static ServerPlayer player;
	private static int ticks;
	private static final int Y = 100;

	static void setup(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "pick-probe"), false);
			player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
			Connection connection = new Connection(PacketFlow.SERVERBOUND);
			new EmbeddedChannel(connection);
			server.getPlayerList().placeNewPlayer(connection, player, cookie);
			player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
			Field f = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
			f.setAccessible(true);
			int id = f.getInt(player.connection);
			var pos = player.position();
			player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id, pos.x, pos.y, pos.z, 0, 0));
			player.absSnapTo(0.5, Y, 0.5, 0, 0);
			player.connection.resetPosition();
			level.getChunkSource().move(player);
			for (int x = 0; x <= 4; x++) {
				for (int y = Y; y <= Y + 3; y++) level.setBlockAndUpdate(new BlockPos(x, y, 0), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(new BlockPos(x, Y - 1, 0), Blocks.STONE.defaultBlockState());
			}
			for (int y = Y; y <= Y + 2; y++) level.setBlockAndUpdate(new BlockPos(1, y, 0), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(2, Y, 0), Blocks.CHEST.defaultBlockState());
			player.getInventory().setItem(3, new ItemStack(Items.CHEST));
			player.getInventory().setSelectedSlot(0);
			ExampleMod.LOGGER.info("PICK_PROBE ready pos={} chest={} selected={} has_infinite={}", player.position(), level.getBlockState(new BlockPos(2, Y, 0)).getBlock(), player.getInventory().getSelectedSlot(), player.hasInfiniteMaterials());
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("PICK_PROBE setup failed", e);
		}
	}

	static void tick(MinecraftServer server) {
		if (player == null) return;
		try {
			if (++ticks == 5) {
				player.connection.handlePickItemFromBlock(new ServerboundPickItemFromBlockPacket(new BlockPos(2, Y, 0), false));
				ExampleMod.LOGGER.info("PICK_PROBE hidden_chest_selected={} held={}", player.getInventory().getSelectedSlot(), player.getMainHandItem());
				player.connection.handlePickItemFromBlock(new ServerboundPickItemFromBlockPacket(new BlockPos(1, Y, 0), false));
				ExampleMod.LOGGER.info("PICK_PROBE hidden_stone_selected={} held={}", player.getInventory().getSelectedSlot(), player.getMainHandItem());
			}
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("PICK_PROBE tick failed", e);
			player = null;
		}
	}
}
