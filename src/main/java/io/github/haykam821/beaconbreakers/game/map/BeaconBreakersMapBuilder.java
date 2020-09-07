package io.github.haykam821.beaconbreakers.game.map;

import java.util.concurrent.CompletableFuture;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

public class BeaconBreakersMapBuilder {
	private final BeaconBreakersMapConfig mapConfig;

	public BeaconBreakersMapBuilder(BeaconBreakersMapConfig mapConfig) {
		this.mapConfig = mapConfig;
	}

	public CompletableFuture<BeaconBreakersMap> create(MinecraftServer server) {
		return CompletableFuture.supplyAsync(() -> {
			return new BeaconBreakersMap(server, this.mapConfig);
		}, Util.getMainWorkerExecutor());
	}
}
