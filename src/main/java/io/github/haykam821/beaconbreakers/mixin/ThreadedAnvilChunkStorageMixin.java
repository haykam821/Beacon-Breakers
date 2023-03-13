package io.github.haykam821.beaconbreakers.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.haykam821.beaconbreakers.game.map.BeaconBreakersChunkGenerator;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
	@Shadow
	@Final
	private ChunkGenerator chunkGenerator;

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/chunk/ChunkGeneratorSettings;createMissingSettings()Lnet/minecraft/world/gen/chunk/ChunkGeneratorSettings;"))
	private ChunkGeneratorSettings useBeaconBreakersChunkGeneratorSettings() {
		if (this.chunkGenerator instanceof BeaconBreakersChunkGenerator beaconBreakersChunkGenerator) {
			ChunkGeneratorSettings settings = beaconBreakersChunkGenerator.getSettings();
			if (settings != null) return settings;
		}

		return ChunkGeneratorSettings.createMissingSettings();
	}
}
