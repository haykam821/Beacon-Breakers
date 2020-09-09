package io.github.haykam821.beaconbreakers.game.map.gen;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

public final class ChaosChunkGenerator extends NoiseChunkGenerator {

    public ChaosChunkGenerator(BiomeSource biomeSource, long worldSeed, Supplier<ChunkGeneratorSettings> supplier) {
        super(biomeSource, worldSeed, supplier);
    }

    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public double sampleNoise(int x, int y, int z, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch) {
        double d = 0.0D;
        double e = 0.0D;
        double f = 0.0D;
        double g = 1.0D;

        for(int i = 0; i < 16; ++i) {
            double h = OctavePerlinNoiseSampler.maintainPrecision((double)x * horizontalScale * g);
            double j = OctavePerlinNoiseSampler.maintainPrecision((double)y * verticalScale * g);
            double k = OctavePerlinNoiseSampler.maintainPrecision((double)z * horizontalScale * g);
            double l = verticalScale * g;
            PerlinNoiseSampler perlinNoiseSampler = this.lowerInterpolatedNoise.getOctave(i);
            if (perlinNoiseSampler != null) {
                d += perlinNoiseSampler.sample(h, j, k, l, (double)y * l) / g;
            }

            PerlinNoiseSampler perlinNoiseSampler2 = this.upperInterpolatedNoise.getOctave(i);
            if (perlinNoiseSampler2 != null) {
                e += perlinNoiseSampler2.sample(h, j, k, l, (double)y * l) / g;
            }

            if (i < 8) {
                PerlinNoiseSampler perlinNoiseSampler3 = this.interpolationNoise.getOctave(i);
                if (perlinNoiseSampler3 != null) {
                    f += perlinNoiseSampler3.sample(OctavePerlinNoiseSampler.maintainPrecision((double)x * horizontalStretch * g), OctavePerlinNoiseSampler.maintainPrecision((double)y * verticalStretch * g), OctavePerlinNoiseSampler.maintainPrecision((double)z * horizontalStretch * g), verticalStretch * g, (double)y * verticalStretch * g) / g;
                }
            }

            g /= 2.0D;
        }

        return MathHelper.clampedLerp(d / 512.0D, e / 512.0D, (f / 32.0D + 1.0D) / 2.0D);
    }

    @Override
    public void sampleNoiseColumn(double[] buffer, int x, int z) {
        GenerationShapeConfig generationShapeConfig = this.settings.get().getGenerationShapeConfig();
        double ai;
        double aj;

        double ae = 684.412D * generationShapeConfig.getSampling().getXZScale();
        double af = 684.412D * generationShapeConfig.getSampling().getYScale();
        double ag = ae / generationShapeConfig.getSampling().getXZFactor();
        double ah = af / generationShapeConfig.getSampling().getYFactor();
        ai = generationShapeConfig.getTopSlide().getTarget();
        aj = generationShapeConfig.getTopSlide().getSize();
        double ak = generationShapeConfig.getTopSlide().getOffset();
        double al = generationShapeConfig.getBottomSlide().getTarget();
        double am = generationShapeConfig.getBottomSlide().getSize();
        double an = generationShapeConfig.getBottomSlide().getOffset();

        for(int ar = 0; ar <= this.noiseSizeY; ++ar) {
            double noise = this.sampleNoise(x, ar, z, ae, af, ag, ah);

            noise += smooth(ar);

            double ax;
            if (aj > 0.0D) {
                ax = ((double)(this.noiseSizeY - ar) - ak) / aj;
                noise = MathHelper.clampedLerp(ai, noise, ax);
            }

            if (am > 0.0D) {
                ax = ((double)ar - an) / am;
                noise = MathHelper.clampedLerp(al, noise, ax);
            }

            buffer[ar] = noise;
        }

    }

    private static double smooth(int y) {
        double x = y / 32.0; // normalize [0, 1]

        double step = x * x * (3 - 2 * x); // smoothstep

        return (-96 * step) + 64; // apply [64, -32] as y -> [0, 32]
    }
}
