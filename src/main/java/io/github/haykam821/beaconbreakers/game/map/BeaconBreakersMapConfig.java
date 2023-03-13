package io.github.haykam821.beaconbreakers.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.WorldPreset;

public class BeaconBreakersMapConfig {
	public static final Codec<BeaconBreakersMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			RegistryFixedCodec.of(RegistryKeys.WORLD_PRESET).fieldOf("preset").forGetter(mapConfig -> mapConfig.worldPreset),
			RegistryKey.createCodec(RegistryKeys.DIMENSION).optionalFieldOf("dimension_options", DimensionOptions.OVERWORLD).forGetter(mapConfig -> mapConfig.dimensionOptions),
			Codec.INT.optionalFieldOf("x", 16).forGetter(BeaconBreakersMapConfig::getX),
			Codec.INT.optionalFieldOf("z", 16).forGetter(BeaconBreakersMapConfig::getZ)
		).apply(instance, BeaconBreakersMapConfig::new);
	});

	private final RegistryEntry<WorldPreset> worldPreset;
	private final RegistryKey<DimensionOptions> dimensionOptions;
	private final int x;
	private final int z;

	public BeaconBreakersMapConfig(RegistryEntry<WorldPreset> worldPreset, RegistryKey<DimensionOptions> dimensionOptions, int x, int z) {
		this.worldPreset = worldPreset;
		this.dimensionOptions = dimensionOptions;
		this.x = x;
		this.z = z;
	}

	public DimensionOptions getDimensionOptions() {
		DimensionOptionsRegistryHolder registryHolder = this.worldPreset.value().createDimensionsRegistryHolder();
		return registryHolder.dimensions().get(this.dimensionOptions);
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}
}
