package io.github.haykam821.beaconbreakers.game.map;

import net.minecraft.server.MinecraftServer;

public final class BeaconBreakersMap {
	private final BeaconBreakersMapConfig mapConfig;
	private final BeaconBreakersChunkGenerator chunkGenerator;

	public BeaconBreakersMap(MinecraftServer server, BeaconBreakersMapConfig mapConfig) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new BeaconBreakersChunkGenerator(server, this.mapConfig);
	}

	public BeaconBreakersChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}
}
