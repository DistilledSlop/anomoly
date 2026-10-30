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
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class TeleportProbe {
	private static ServerPlayer player;
	private static Pig target;
	private static Warden kineticTarget;
	private static int ticks;
	private static final int Y = 100;
	private static final double Z = 0.5;

	static void setup(MinecraftServer server) {
		try {
			ServerLevel level = server.overworld();
			CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "teleport-probe"), false);
			player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
			Connection connection = new Connection(PacketFlow.SERVERBOUND);
			new EmbeddedChannel(connection);
			server.getPlayerList().placeNewPlayer(connection, player, cookie);
			player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
			ackCurrentTeleport(player.position());
			player.absSnapTo(0.5, Y, Z, 0, 0);
			level.getChunkSource().move(player);
			for (int x = -1; x <= 24; x++) {
				for (int z = -1; z <= 1; z++) {
					level.setBlockAndUpdate(new BlockPos(x, Y - 1, z), Blocks.STONE.defaultBlockState());
					for (int y = Y; y <= Y + 9; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
			level.setBlockAndUpdate(new BlockPos(11, Y, 0), Blocks.CHEST.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(19, Y, 0), Blocks.CHEST.defaultBlockState());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SPEAR));
			target = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
			if (target == null) throw new IllegalStateException("pig creation failed");
			target.setNoAi(true);
			target.setPos(17.5, Y, Z);
			level.addFreshEntity(target);
			for (int x = 15; x <= 17; x++) {
				for (int z = -1; z <= 1; z++) level.setBlockAndUpdate(new BlockPos(x, Y + 2, z), Blocks.STONE.defaultBlockState());
			}
			ExampleMod.LOGGER.info("TP_PROBE ready pos={} menu={}", player.position(), player.containerMenu.getClass().getSimpleName());
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("TP_PROBE setup failed", e);
		}
	}

	static void tick(MinecraftServer server) {
		if (player == null) return;
		try {
			++ticks;
			if (Boolean.getBoolean("mc.research.spearOnly")) player.connection.tick();
			if (Boolean.getBoolean("mc.research.spearOnly") && ticks >= 10 && ticks <= 18) return;
			if (ticks == 5) {
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(8.5, Y, Z, true, false));
				ExampleMod.LOGGER.info("TP_PROBE clean_roundtrip_mid={} pending={}", player.position(), pendingPosition());
				BlockPos remoteChest = new BlockPos(11, Y, 0);
				player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(remoteChest), Direction.UP, remoteChest, false), 3));
				ExampleMod.LOGGER.info("TP_PROBE clean_roundtrip_chest={} pos={}", player.containerMenu.getClass().getSimpleName(), player.position());
				player.doCloseContainer();
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(0.5, Y, Z, true, false));
				ExampleMod.LOGGER.info("TP_PROBE clean_roundtrip_end={} pending={}", player.position(), pendingPosition());
			} else if (ticks == 6) {
				ExampleMod.LOGGER.info("TP_PROBE clean_roundtrip_next_tick={}", player.position());
			} else if (ticks == 10) {
				player.connection.teleport(0.5, Y, Z, 0, 0);
				ExampleMod.LOGGER.info("TP_PROBE issued teleport expected={} id={}", player.position(), teleportId());
			} else if (ticks == 11) {
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(0.5, Y, Z, true, false));
				ackCurrentTeleport(new Vec3(8.5, Y, Z));
				ExampleMod.LOGGER.info("TP_PROBE ack_offset_8 pos={} pending={}", player.position(), pendingPosition());
				BlockPos chest = new BlockPos(11, Y, 0);
				player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(chest), Direction.UP, chest, false), 1));
				ExampleMod.LOGGER.info("TP_PROBE remote_chest_menu={} pos={}", player.containerMenu.getClass().getSimpleName(), player.position());
				player.doCloseContainer();
				player.level().setBlockAndUpdate(new BlockPos(11, Y, 0), Blocks.AIR.defaultBlockState());
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(1000.5, Y, Z, true, false));
				ExampleMod.LOGGER.info("TP_PROBE forced_correction pos={} pending={} id={}", player.position(), pendingPosition(), teleportId());
				ackCurrentTeleport(new Vec3(16.5, Y, Z));
				ExampleMod.LOGGER.info("TP_PROBE same_tick_chained_ack pos={} pending={}", player.position(), pendingPosition());
				BlockPos secondChest = new BlockPos(19, Y, 0);
				player.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(secondChest), Direction.UP, secondChest, false), 2));
				ExampleMod.LOGGER.info("TP_PROBE chained_chest_menu={} pos={}", player.containerMenu.getClass().getSimpleName(), player.position());
			} else if (ticks == 12) {
				ExampleMod.LOGGER.info("TP_PROBE next_tick_pos={}", player.position());
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(16.5, Y + 4, Z, false, false));
				ExampleMod.LOGGER.info("TP_PROBE vertical_roof_cross_pos={} roof={} pending={}", player.position(), player.level().getBlockState(new BlockPos(16, Y + 2, 0)).getBlock(), pendingPosition());
			} else if (ticks == 13) {
				ExampleMod.LOGGER.info("TP_PROBE vertical_next_tick_pos={}", player.position());
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(16.5, Y, Z, true, false));
				ExampleMod.LOGGER.info("TP_PROBE vertical_return_pos={} roof={} pending={}", player.position(), player.level().getBlockState(new BlockPos(16, Y + 2, 0)).getBlock(), pendingPosition());
			} else if (ticks == 14) {
				ExampleMod.LOGGER.info("TP_PROBE vertical_return_next_tick_pos={}", player.position());
				for (int x = 15; x <= 17; x++) {
					for (int z = -1; z <= 1; z++) {
						for (int y = Y + 2; y <= Y + 4; y++) player.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.BEDROCK.defaultBlockState());
					}
				}
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(16.5, Y + 6, Z, false, false));
				ExampleMod.LOGGER.info("TP_PROBE bedrock_roof_cross_pos={} center={} pending={}", player.position(), player.level().getBlockState(new BlockPos(16, Y + 3, 0)).getBlock(), pendingPosition());
			} else if (ticks == 15) {
				ExampleMod.LOGGER.info("TP_PROBE bedrock_next_tick_pos={}", player.position());
			} else if (ticks == 16) {
				player.absSnapTo(0.5, Y, Z, -90, 7);
				player.setYHeadRot(-90);
				player.level().getChunkSource().move(player);
				player.connection.resetPosition();
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				ExampleMod.LOGGER.info("TP_PROBE spear_reset_pos={} target_health={}", player.position(), target.getHealth());
			} else if (ticks == 24) {
				player.absSnapRotationTo(-90, 7);
				player.setYHeadRot(-90);
				target = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
				if (target == null) throw new IllegalStateException("pig creation failed");
				target.setNoAi(true);
				target.setPos(17.5, Y, Z);
				player.level().getChunk(1, 0);
				ExampleMod.LOGGER.info("TP_PROBE spear_spawn added={} entity_id={}", player.level().addFreshEntity(target), target.getId());
			} else if (ticks == 25) {
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(8.5, Y, Z, -90, 7, true, false));
				ExampleMod.LOGGER.info("TP_PROBE spear_advance pos={} known={} target_health={}", player.position(), player.getKnownMovement(), target.getHealth());
				var spearRay = ProjectileUtil.getHitEntitiesAlong(player, player.getAttackRangeWith(player.getMainHandItem()), e -> PiercingWeapon.canHitEntity(player, e), ClipContext.Block.COLLIDER);
				ExampleMod.LOGGER.info("TP_PROBE spear_ray head={} look={} target={} box={} live={} registered={} reach={} block={} entities={}", player.getYHeadRot(), player.getHeadLookAngle(), target.position(), target.getBoundingBox(), target.isAlive(), player.level().getEntity(target.getId()) != null, player.getAttackRangeWith(player.getMainHandItem()).effectiveMaxRange(player), spearRay.left().map(h -> h.getBlockPos() + " @ " + h.getLocation()), spearRay.right().map(java.util.Collection::size));
				player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.UP));
				ExampleMod.LOGGER.info("TP_PROBE spear_stab pos={} target_health={}", player.position(), target.getHealth());
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				var zeroRay = ProjectileUtil.getHitEntitiesAlong(player, player.getAttackRangeWith(player.getMainHandItem()), e -> PiercingWeapon.canHitEntity(player, e), ClipContext.Block.COLLIDER);
				ExampleMod.LOGGER.info("TP_PROBE spear_zero_known={} zero_ray_entities={}", player.getKnownMovement(), zeroRay.right().map(java.util.Collection::size));
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(0.5, Y, Z, true, false));
				ExampleMod.LOGGER.info("TP_PROBE spear_return pos={} pending={}", player.position(), pendingPosition());
			} else if (ticks == 26) {
				ExampleMod.LOGGER.info("TP_PROBE spear_next_tick_pos={} target_health={}", player.position(), target.getHealth());
			} else if (ticks == 30) {
				player.startUsingItem(InteractionHand.MAIN_HAND);
				ExampleMod.LOGGER.info("TP_PROBE kinetic_started pos={} using={} player_tick={} remaining={}", player.position(), player.isUsingItem(), player.tickCount, player.getUseItemRemainingTicks());
			} else if (ticks == 35 || ticks == 40) {
				ExampleMod.LOGGER.info("TP_PROBE kinetic_timer server_tick={} player_tick={} remaining={} using={}", ticks, player.tickCount, player.getUseItemRemainingTicks(), player.isUsingItem());
			} else if (ticks == 44) {
				kineticTarget = EntityTypes.WARDEN.create(player.level(), EntitySpawnReason.COMMAND);
				if (kineticTarget == null) throw new IllegalStateException("warden creation failed");
				kineticTarget.setNoAi(true);
				kineticTarget.setPos(17.5, Y, Z);
				player.level().getChunk(1, 0);
				ExampleMod.LOGGER.info("TP_PROBE kinetic_spawn added={} id={}", player.level().addFreshEntity(kineticTarget), kineticTarget.getId());
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.PosRot(8.5, Y, Z, -90, 7, true, false));
				player.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
				var kineticRay = ProjectileUtil.getHitEntitiesAlong(player, player.getAttackRangeWith(player.getMainHandItem()), e -> PiercingWeapon.canHitEntity(player, e), ClipContext.Block.COLLIDER);
				ExampleMod.LOGGER.info("TP_PROBE kinetic_advance pos={} known={} target_health={} player_tick={} remaining={} head={} ray_entities={}", player.position(), player.getKnownMovement(), kineticTarget.getHealth(), player.tickCount, player.getUseItemRemainingTicks(), player.getYHeadRot(), kineticRay.right().map(java.util.Collection::size));
			} else if (ticks == 45) {
				ExampleMod.LOGGER.info("TP_PROBE kinetic_next_tick target_health={} alive={} player_known={} using={} player_tick={} remaining={} head={}", kineticTarget.getHealth(), kineticTarget.isAlive(), player.getKnownMovement(), player.isUsingItem(), player.tickCount, player.getUseItemRemainingTicks(), player.getYHeadRot());
			}
		} catch (Throwable e) {
			ExampleMod.LOGGER.error("TP_PROBE tick failed at {}", ticks, e);
			player = null;
		}
	}

	private static void ackCurrentTeleport(Vec3 pos) throws Exception {
		player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(teleportId(), pos.x, pos.y, pos.z, 0, 0));
	}

	private static int teleportId() throws Exception {
		Field f = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");
		f.setAccessible(true);
		return f.getInt(player.connection);
	}

	private static Vec3 pendingPosition() throws Exception {
		Field f = ServerGamePacketListenerImpl.class.getDeclaredField("awaitingPositionFromClient");
		f.setAccessible(true);
		return (Vec3)f.get(player.connection);
	}
}
