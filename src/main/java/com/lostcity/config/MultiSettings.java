package com.lostcity.config;

/**
 * Настройки для мульти-зданий (multibuildings).
 * Оригинал: mcjty.lostcities.worldgen.lost.regassets.data.MultiSettings
 * 
 * @param areasize Размер области (NxN чанков), в которой размещаются мульти-здания
 * @param minimum Минимальное количество мульти-зданий в области
 * @param maximum Максимальное количество мульти-зданий в области
 * @param correctStyleFactor Минимальная доля чанков правильного стиля (0.8 = 80%)
 * @param attempts Количество попыток размещения каждого мульти-здания
 */
public record MultiSettings(
    int areasize,
    int minimum,
    int maximum,
    float correctStyleFactor,
    int attempts
) {
    /** Дефолтные настройки: область 10x10, 1-5 зданий, 80% стиля, 50 попыток. */
    public static final MultiSettings DEFAULT = new MultiSettings(10, 1, 5, 0.8f, 50);
}
