package io.github.haykam821.beaconbreakers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.gen.chunk.ChunkGenerator;

@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {
	@Accessor("worldSeed")
	public long getWorldSeed();
}
