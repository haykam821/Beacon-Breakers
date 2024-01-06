package io.github.haykam821.beaconbreakers.game.map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

public final class BeaconBreakersChunkGenerator extends GameChunkGenerator {
	private static final BlockState BARRIER = Blocks.BARRIER.getDefaultState();

	private final BeaconBreakersMapConfig mapConfig;
	private final ChunkGenerator chunkGenerator;

	public BeaconBreakersChunkGenerator(MinecraftServer server, BeaconBreakersMapConfig mapConfig) {
		super(mapConfig.getChunkGenerator().getBiomeSource());
		this.mapConfig = mapConfig;

		this.chunkGenerator = mapConfig.getChunkGenerator();
	}

	public ChunkGeneratorSettings getSettings() {
		if (this.chunkGenerator instanceof NoiseChunkGenerator noiseChunkGenerator) {
			return noiseChunkGenerator.getSettings().value();
		}

		return null;
	}

	private boolean isChunkPosWithinArea(ChunkPos chunkPos) {
		return chunkPos.x >= 0 && chunkPos.z >= 0 && chunkPos.x < this.mapConfig.getX() && chunkPos.z < this.mapConfig.getZ();
	}

	private boolean isChunkWithinArea(Chunk chunk) {
		return this.isChunkPosWithinArea(chunk.getPos());
	}

	@Override
	public CompletableFuture<Chunk> populateBiomes(Executor executor, NoiseConfig noiseConfig, Blender blender, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.populateBiomes(executor, noiseConfig, blender, structures, chunk);
		} else {
			return super.populateBiomes(executor, noiseConfig, blender, structures, chunk);
		}
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.populateNoise(executor, blender, noiseConfig, structures, chunk);
		}
		return super.populateNoise(executor, blender, noiseConfig, structures, chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.buildSurface(region, structures, noiseConfig, chunk);
		}
	}

	@Override
	public BiomeSource getBiomeSource() {
		return this.chunkGenerator.getBiomeSource();
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structures) {
		ChunkPos chunkPos = chunk.getPos();
		if (!this.isChunkPosWithinArea(chunkPos)) return;

		this.chunkGenerator.generateFeatures(world, chunk, structures);

		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;
	
		BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
		BlockPos.Mutable mutablePos = new BlockPos.Mutable();

		int bottomY = chunk.getBottomY();
		int topY = chunk.getTopY() - 1;

		// Top
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				mutablePos.set(x + pos.getX(), topY, z + pos.getZ());
				chunk.setBlockState(mutablePos, BARRIER, false);
			}
		}

		// North
		if (chunkZ == 0) {
			for (int x = 0; x < 16; x++) {
				for (int y = bottomY; y < topY; y++) {
					mutablePos.set(x + pos.getX(), y, pos.getZ());
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}

		// East
		if (chunkX == this.mapConfig.getX() - 1) {
			for (int z = 0; z < 16; z++) {
				for (int y = bottomY; y < topY; y++) {
					mutablePos.set(pos.getX() + 15, y, z + pos.getZ());
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}

		// South
		if (chunkZ == this.mapConfig.getZ() - 1) {
			for (int x = 0; x < 16; x++) {
				for (int y = bottomY; y < topY; y++) {
					mutablePos.set(x + pos.getX(), y, pos.getZ() + 15);
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}

		// West
		if (chunkX == 0) {
			for (int z = 0; z < 16; z++) {
				for (int y = bottomY; y < topY; y++) {
					mutablePos.set(pos.getX(), y, z + pos.getZ());
					chunk.setBlockState(mutablePos, BARRIER, false);
				}
			}
		}
	}

	@Override
	public void carve(ChunkRegion region, long seed, NoiseConfig noiseConfig, BiomeAccess access, StructureAccessor structures, Chunk chunk, Carver carver) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.carve(region, seed, noiseConfig, access, structures, chunk, carver);
		}
	}

	@Override
	public int getSeaLevel() {
		return this.chunkGenerator.getSeaLevel();
	}

	@Override
	public int getMinimumY() {
		return this.chunkGenerator.getMinimumY();
	}

	@Override
	public int getWorldHeight() {
		return this.chunkGenerator.getWorldHeight();
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world, NoiseConfig noiseConfig) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getHeight(x, z, heightmapType, world, noiseConfig);
		}
		return super.getHeight(x, z, heightmapType, world, noiseConfig);
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getColumnSample(x, z, world, noiseConfig);
		}
		return super.getColumnSample(x, z, world, noiseConfig);
	}
}
