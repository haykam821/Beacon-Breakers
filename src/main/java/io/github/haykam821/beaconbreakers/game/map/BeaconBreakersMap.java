package io.github.haykam821.beaconbreakers.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.dimension.DimensionType;

public final class BeaconBreakersMap {
	private final BeaconBreakersMapConfig mapConfig;
	private final BeaconBreakersChunkGenerator chunkGenerator;
	private final BlockBox box;

	public BeaconBreakersMap(MinecraftServer server, BeaconBreakersMapConfig mapConfig, DimensionType dimensionType) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new BeaconBreakersChunkGenerator(server, this.mapConfig);

		int minY = dimensionType.minY();
		int maxY = minY + dimensionType.height();

		this.box = new BlockBox(1, minY + 1, 1, mapConfig.getX() * 16 - 2, maxY - 1, mapConfig.getZ() * 16 - 2);
	}

	public BeaconBreakersChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}

	public BlockBox getBox() {
		return this.box;
	}
}
