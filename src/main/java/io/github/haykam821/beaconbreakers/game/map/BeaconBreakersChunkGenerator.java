package io.github.haykam821.beaconbreakers.game.map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.github.haykam821.beaconbreakers.mixin.ChunkGeneratorAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

public final class BeaconBreakersChunkGenerator extends GameChunkGenerator {
	private static final BlockState BARRIER = Blocks.BARRIER.getDefaultState();

	private final BeaconBreakersMapConfig mapConfig;
	private final long seed;
	private final ChunkGenerator chunkGenerator;

	public BeaconBreakersChunkGenerator(MinecraftServer server, BeaconBreakersMapConfig mapConfig, ChunkGenerator chunkGenerator) {
		super(server);
		this.mapConfig = mapConfig;

		this.seed = ((ChunkGeneratorAccessor) chunkGenerator).getWorldSeed();
		this.chunkGenerator = chunkGenerator;
	}

	public BeaconBreakersChunkGenerator(MinecraftServer server, BeaconBreakersMapConfig mapConfig) {
		this(server, mapConfig, BeaconBreakersChunkGenerator.createChunkGenerator(server, mapConfig));
	}

	private static ChunkGenerator createChunkGenerator(MinecraftServer server, BeaconBreakersMapConfig mapConfig) {
		long seed = server.getOverworld().getRandom().nextLong();
		BiomeSource biomeSource = new VanillaLayeredBiomeSource(seed, false, false, server.getRegistryManager().get(Registry.BIOME_KEY));
		
		ChunkGeneratorSettings chunkGeneratorSettings = BuiltinRegistries.CHUNK_GENERATOR_SETTINGS.get(mapConfig.getChunkGeneratorSettingsId());
		return new NoiseChunkGenerator(biomeSource, seed, () -> chunkGeneratorSettings);
	}

	private boolean isChunkPosWithinArea(ChunkPos chunkPos) {
		return chunkPos.x >= 0 && chunkPos.z >= 0 && chunkPos.x < this.mapConfig.getX() && chunkPos.z < this.mapConfig.getZ();
	}

	private boolean isChunkWithinArea(Chunk chunk) {
		return this.isChunkPosWithinArea(chunk.getPos());
	}

	@Override
	public void populateBiomes(Registry<Biome> registry, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.populateBiomes(registry, chunk);
		} else {
			super.populateBiomes(registry, chunk);
		}
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.populateNoise(executor, structures, chunk);
		}
		return super.populateNoise(executor, structures, chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.buildSurface(region, chunk);
		}
	}

	@Override
	public BiomeSource getBiomeSource() {
		return this.chunkGenerator.getBiomeSource();
	}

	@Override
	public void generateFeatures(ChunkRegion region, StructureAccessor structures) {
		int chunkX = region.getCenterPos().x;
		int chunkZ = region.getCenterPos().z;

		ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
		if (!this.isChunkPosWithinArea(chunkPos)) return;
	
		BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
		Biome biome = this.chunkGenerator.getBiomeSource().getBiomeForNoiseGen((chunkX << 2) + 2, 2, (chunkZ << 2) + 2);
	
		ChunkRandom chunkRandom = new ChunkRandom();
		long populationSeed = chunkRandom.setPopulationSeed(this.seed, pos.getX(), pos.getZ());
		
		biome.generateFeatureStep(structures, this.chunkGenerator, region, populationSeed, chunkRandom, pos);

		BlockPos.Mutable mutablePos = new BlockPos.Mutable();
		Chunk chunk = region.getChunk(chunkPos.getStartPos());

		// Top
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				mutablePos.set(x + pos.getX(), 255, z + pos.getZ());
				chunk.setBlockState(mutablePos, BARRIER, false);
			}
		}

		// North
		if (chunkZ == 0) {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 256; y++) {
					mutablePos.set(x + pos.getX(), y, pos.getZ());
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}

		// East
		if (chunkX == this.mapConfig.getX() - 1) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					mutablePos.set(pos.getX() + 15, y, z + pos.getZ());
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}

		// South
		if (chunkZ == this.mapConfig.getZ() - 1) {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 256; y++) {
					mutablePos.set(x + pos.getX(), y, pos.getZ() + 15);
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}

		// West
		if (chunkX == 0) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					mutablePos.set(pos.getX(), y, z + pos.getZ());
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}
	}

	@Override
	public void carve(long seed, BiomeAccess access, Chunk chunk, Carver carver) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.carve(this.seed, access, chunk, carver);
		}
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getHeight(x, z, heightmapType, world);
		}
		return super.getHeight(x, z, heightmapType, world);
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getColumnSample(x, z, world);
		}
		return super.getColumnSample(x, z, world);
	}
}
