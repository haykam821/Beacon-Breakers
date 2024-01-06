package io.github.haykam821.beaconbreakers.game.map;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.NoiseHypercube;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

public class BeaconBreakersMapConfig {
	public static final Codec<BeaconBreakersMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			RegistryFixedCodec.of(RegistryKeys.WORLD_PRESET).fieldOf("preset").forGetter(mapConfig -> mapConfig.worldPreset),
			RegistryKey.createCodec(RegistryKeys.DIMENSION).optionalFieldOf("dimension_options", DimensionOptions.OVERWORLD).forGetter(mapConfig -> mapConfig.dimensionOptions),
			RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("excluded_biomes").forGetter(mapConfig -> mapConfig.excludedBiomes),
			Codec.INT.optionalFieldOf("x", 16).forGetter(BeaconBreakersMapConfig::getX),
			Codec.INT.optionalFieldOf("z", 16).forGetter(BeaconBreakersMapConfig::getZ)
		).apply(instance, BeaconBreakersMapConfig::new);
	});

	private final RegistryEntry<WorldPreset> worldPreset;
	private final RegistryKey<DimensionOptions> dimensionOptions;
	private final Optional<RegistryEntryList<Biome>> excludedBiomes;

	private final int x;
	private final int z;

	private ChunkGenerator chunkGenerator;

	public BeaconBreakersMapConfig(RegistryEntry<WorldPreset> worldPreset, RegistryKey<DimensionOptions> dimensionOptions, Optional<RegistryEntryList<Biome>> excludedBiomes, int x, int z) {
		this.worldPreset = worldPreset;
		this.dimensionOptions = dimensionOptions;
		this.excludedBiomes = excludedBiomes;

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

	public ChunkGenerator getChunkGenerator() {
		if (this.chunkGenerator == null) {
			this.chunkGenerator = this.createChunkGenerator();
		}

		return this.chunkGenerator;
	}

	private boolean isIncludedBiome(Pair<NoiseHypercube, RegistryEntry<Biome>> pair) {
		return this.excludedBiomes.isEmpty() || !this.excludedBiomes.get().contains(pair.getSecond());
	}

	private ChunkGenerator createChunkGenerator() {
		DimensionOptions dimensionOptions = this.getDimensionOptions();

		if (this.excludedBiomes.isPresent()) {
			if (dimensionOptions.chunkGenerator() instanceof NoiseChunkGenerator noiseChunkGenerator) {
				if (noiseChunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource biomeSource) {
					List<Pair<NoiseHypercube, RegistryEntry<Biome>>> entries = biomeSource.getBiomeEntries()
						.getEntries()
						.stream()
						.filter(this::isIncludedBiome)
						.toList();

					MultiNoiseBiomeSource newBiomeSource = MultiNoiseBiomeSource.create(new MultiNoiseUtil.Entries<>(entries));

					return new NoiseChunkGenerator(newBiomeSource, noiseChunkGenerator.getSettings());
				}

				throw new IllegalArgumentException("Cannot exclude biomes from unsupported biome source");
			}

			throw new IllegalArgumentException("Cannot exclude biomes from unsupported chunk generator");
		}

		return dimensionOptions.chunkGenerator();
	}
}
