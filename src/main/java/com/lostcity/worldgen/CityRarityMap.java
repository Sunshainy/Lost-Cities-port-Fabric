package com.lostcity.worldgen;

import com.lostcity.util.PerlinNoiseGenerator14;

/**
 * Карта редкости городов на основе Perlin noise.
 * Портировано из mcjty.lostcities.worldgen.lost.CityRarityMap (оригинальный Forge мод).
 * 
 * Используется когда CITY_CHANCE < 0 для процедурной генерации городов через Perlin noise.
 */
public class CityRarityMap {

    private final PerlinNoiseGenerator14 perlinCity;
    private final double scale;
    private final double offset;
    private final double innerScale;

    public CityRarityMap(long seed, double scale, double offset, double innerScale) {
        perlinCity = new PerlinNoiseGenerator14(seed, 4);
        this.scale = scale;
        this.offset = offset;
        this.innerScale = innerScale;
    }

    /**
     * Получить city factor для чанка на основе Perlin noise.
     * Оригинал: getCityFactor() в CityRarityMap.java
     */
    public float getCityFactor(int cx, int cz) {
        double factor = perlinCity.getValue(cx / scale, cz / scale) * innerScale - offset;
        if (factor < 0) {
            factor = 0;
        }
        return (float) factor;
    }
}
