package com.lostcity.util;

import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.noise.SimplexNoiseSampler;

/**
 * Октавный Simplex-шум, как в оригинале Lost Cities (PerlinNoiseGenerator14).
 * Используется для Highway и City factor. Seed — мир.
 */
public final class PerlinNoiseGenerator14 {

    private final SimplexNoiseSampler[] levels;
    private final int levelsCount;

    public PerlinNoiseGenerator14(long seed, int levelsIn) {
        this.levelsCount = levelsIn;
        this.levels = new SimplexNoiseSampler[levelsIn];
        for (int i = 0; i < levelsIn; i++) {
            Random r = Random.create(seed);
            this.levels[i] = new SimplexNoiseSampler(r);
        }
    }

    /**
     * Как в оригинале: октавы с масштабом 1, 1/2, 1/4, ...
     */
    public double getValue(double x, double z) {
        double sum = 0;
        double scale = 1.0;
        for (int i = 0; i < levelsCount; i++) {
            sum += levels[i].sample(x * scale, z * scale) / scale;
            scale *= 0.5;
        }
        return sum;
    }
}
