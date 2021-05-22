package io.github.haykam821.beaconbreakers.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class PlayerEntry {
	private static final String UNPLACED_BEACON_STRING = "" + Formatting.YELLOW + Formatting.BOLD + "⌛";
	private static final String PLACED_BEACON_STRING = "" + Formatting.GREEN + Formatting.BOLD + "✔";
	private static final String NO_BEACON_STRING = "" + Formatting.RED + Formatting.BOLD + "❌";

	private final ServerPlayerEntity player;
	private BlockPos beaconPos;
	private boolean beaconBroken = false;

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

	public void setBeaconBroken() {
		this.beaconBroken = true;
	}

	public String getSidebarEntryString() {
		return this.getSidebarEntryIcon() + " " + Formatting.RESET + this.player.getEntityName();
	}

	public String getSidebarEntryIcon() {
		if (this.beaconPos == null) {
			return PlayerEntry.UNPLACED_BEACON_STRING;
		} else if (this.beaconBroken) {
			return PlayerEntry.NO_BEACON_STRING;
		} else {
			return PlayerEntry.PLACED_BEACON_STRING; 
		}
	}

	@Override
	public String toString() {
		return "PlayerEntry{player=" + this.getPlayer() + ", beaconPos=" + this.getBeaconPos() + "}";
	}
}