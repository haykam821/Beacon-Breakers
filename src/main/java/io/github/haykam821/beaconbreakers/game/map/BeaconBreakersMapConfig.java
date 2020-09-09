package io.github.haykam821.beaconbreakers.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Identifier;

public class BeaconBreakersMapConfig {
	public static final Codec<BeaconBreakersMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.fieldOf("settings").forGetter(BeaconBreakersMapConfig::getChunkGeneratorSettingsId),
			Codec.INT.optionalFieldOf("x", 16).forGetter(BeaconBreakersMapConfig::getX),
			Codec.INT.optionalFieldOf("z", 16).forGetter(BeaconBreakersMapConfig::getZ),
			Codec.BOOL.optionalFieldOf("chaos", false).forGetter(BeaconBreakersMapConfig::useChaoticGenerator)
		).apply(instance, BeaconBreakersMapConfig::new);
	});

	private final Identifier chunkGeneratorSettingsId;
	private final int x;
	private final int z;
	private final boolean chaos;

	public BeaconBreakersMapConfig(Identifier chunkGeneratorSettingsId, int x, int z, boolean chaos) {
		this.chunkGeneratorSettingsId = chunkGeneratorSettingsId;
		this.x = x;
		this.z = z;
		this.chaos = chaos;
	}

	public Identifier getChunkGeneratorSettingsId() {
		return this.chunkGeneratorSettingsId;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}

	public boolean useChaoticGenerator() {
		return this.chaos;
	}
}
