package io.github.haykam821.beaconbreakers.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class PlayerEntry {
	private final ServerPlayerEntity player;
	private BlockPos beaconPos;

	public PlayerEntry(ServerPlayerEntity player) {
		this.player = player;
	}

	public ServerPlayerEntity getPlayer() {
		return player;
	}

	public BlockPos getBeaconPos() {
		return this.beaconPos;
	}

	public void setBeaconPos(BlockPos beaconPos) {
		this.beaconPos = beaconPos;
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.getPlayer() + ", beaconPos=" + this.getBeaconPos() + "}";
	}
}