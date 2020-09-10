package io.github.haykam821.beaconbreakers.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockBox;

public final class BeaconBreakersMap {
	private final BeaconBreakersMapConfig mapConfig;
	private final BeaconBreakersChunkGenerator chunkGenerator;
	private final BlockBox box;

	public BeaconBreakersMap(MinecraftServer server, BeaconBreakersMapConfig mapConfig) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new BeaconBreakersChunkGenerator(server, this.mapConfig);
		this.box = new BlockBox(1, 1, 1, mapConfig.getX() * 16 - 2, 254, mapConfig.getZ() * 16 - 2);
	}

	public BeaconBreakersChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}

	public BlockBox getBox() {
		return this.box;
	}
}
