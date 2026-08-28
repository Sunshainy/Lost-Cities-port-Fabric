package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.config.LostCityConfig;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.PerlinNoiseGenerator14;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Магистрали (highways). Оригинал: mcjty.lostcities.worldgen.lost.Highway.
 * Октавный Simplex-шум (PerlinNoiseGenerator14) + маска по чанкам; уровень от city level конечных городов.
 */
public final class Highway {

    // ConcurrentHashMap, как в оригинале: getHighwayLevel вызывается из нескольких Worker-Main
    // потоков одновременно (особенно при пре-генерации Chunky). Обычный HashMap здесь
    // мог отдавать мусор или зависать на resize.
    private static final Map<DimChunk, Integer> X_LEVEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<DimChunk, Integer> Z_LEVEL_CACHE = new ConcurrentHashMap<>();

    /** Шум на измерение+сид, как HIGHWAY_NOISE в оригинале. */
    private record Noise(PerlinNoiseGenerator14 x, PerlinNoiseGenerator14 z) {
    }

    private static final Map<Long, Noise> NOISE = new ConcurrentHashMap<>();

    private static Noise getNoise(long seed) {
        return NOISE.computeIfAbsent(seed,
                s -> new Noise(new PerlinNoiseGenerator14(s, 4), new PerlinNoiseGenerator14(s, 4)));
    }

    /** Как в оригинале: perlinX.getValue(cx/MAIN, cz/SECONDARY). */
    private static double noiseX(Noise noise, int chunkX, int chunkZ, ProfileConfig profile) {
        double sx = chunkX / profile.getHighwayMainPerlinScale();
        double sz = chunkZ / profile.getHighwaySecondaryPerlinScale();
        return noise.x().getValue(sx, sz);
    }

    /** Как в оригинале: perlinZ.getValue(cx/SECONDARY, cz/MAIN). */
    private static double noiseZ(Noise noise, int chunkX, int chunkZ, ProfileConfig profile) {
        double sx = chunkX / profile.getHighwaySecondaryPerlinScale();
        double sz = chunkZ / profile.getHighwayMainPerlinScale();
        return noise.z().getValue(sx, sz);
    }

    private static boolean hasHighway(Noise noise, int chunkX, int chunkZ, ProfileConfig profile, boolean useX) {
        double value = useX ? noiseX(noise, chunkX, chunkZ, profile) : noiseZ(noise, chunkX, chunkZ, profile);
        return value > profile.getHighwayPerlinFactor();
    }

    /**
     * Уровень X-магистрали в чанке: -1 нет, 0/1 уровень.
     * Оригинал: getXHighwayLevel.
     */
    public static int getXHighwayLevel(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        return getHighwayLevel(pos, profile, world, true);
    }

    public static int getZHighwayLevel(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        return getHighwayLevel(pos, profile, world, false);
    }

    private static int getHighwayLevel(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world, boolean useX) {
        StructureWorldAccess w = world != null ? world : ChunkHeightmap.getCurrentWorld();
        Map<DimChunk, Integer> cache = useX ? X_LEVEL_CACHE : Z_LEVEL_CACHE;
        DimChunk k = DimChunk.of(w, pos.x, pos.z);
        Integer cachedLevel = cache.get(k);
        if (cachedLevel != null) return cachedLevel;

        int mask = profile.getHighwayDistanceMask();
        if (mask <= 0) {
            cache.put(k, -1);
            return -1;
        }

        int otherCoord = useX ? pos.z : pos.x;
        if ((otherCoord & mask) != 0) {
            cache.put(k, -1);
            return -1;
        }

        LostCityConfig config = LostCityMod.getConfig();
        if (config == null) {
            // Не кэшируем: конфиг появится позже, а -1 залипнет навсегда.
            return -1;
        }

        if (w == null) {
            // Нет мира — высоту концов магистрали не посчитать. Не кэшируем.
            return -1;
        }
        long seed = w.toServerWorld().getSeed();
        Noise noise = getNoise(seed);

        // Оригинал: ищем lower/higher только если текущий чанк входит в highway (perlin > factor)
        boolean hasCurrent = hasHighway(noise, pos.x, pos.z, profile, useX);
        if (!hasCurrent) {
            cache.put(k, -1);
            return -1;
        }

        int lower = useX ? pos.x : pos.z;
        while (lower >= -10000) {
            int cx = useX ? lower : pos.x;
            int cz = useX ? pos.z : lower;
            if (!hasHighway(noise, cx, cz, profile, useX)) break;
            lower--;
        }
        lower++;

        int higher = useX ? pos.x : pos.z;
        while (higher <= 10000) {
            int cx = useX ? higher : pos.x;
            int cz = useX ? pos.z : higher;
            if (!hasHighway(noise, cx, cz, profile, useX)) break;
            higher++;
        }
        higher--;

        if (higher - lower < 5) {
            cache.put(k, -1);
            return -1;
        }

        ChunkPos lowerPos = useX ? new ChunkPos(lower, pos.z) : new ChunkPos(pos.x, lower);
        ChunkPos higherPos = useX ? new ChunkPos(higher, pos.z) : new ChunkPos(pos.x, higher);
        // Концы магистрали лежат далеко за пределами текущего региона — высота для них
        // берётся из шума генератора (ChunkHeightmap -> TerrainHeight), а не через чанки.
        boolean cityLower = City.isCity(lowerPos, config, w);
        boolean cityHigher = City.isCity(higherPos, config, w);
        boolean valid = profile.getHighwayRequiresTwoCities() ? (cityLower && cityHigher) : (cityLower || cityHigher);
        if (!valid) {
            cache.put(k, -1);
            return -1;
        }

        int levelLower = ChunkHeightmap.getCityLevel(lowerPos, profile, w);
        int levelHigher = ChunkHeightmap.getCityLevel(higherPos, profile, w);
        int level = switch (profile.getHighwayLevelFromCitiesMode()) {
            case 0 -> levelLower;
            case 1 -> Math.min(levelLower, levelHigher);
            case 2 -> Math.max(levelLower, levelHigher);
            case 3 -> (levelLower + levelHigher) / 2;
            default -> levelLower;
        };

        for (int i = lower; i <= higher; i++) {
            DimChunk key = useX ? DimChunk.of(w, i, pos.z) : DimChunk.of(w, pos.x, i);
            cache.put(key, level);
        }
        return level;
    }

    public static void cleanCache() {
        X_LEVEL_CACHE.clear();
        Z_LEVEL_CACHE.clear();
        NOISE.clear();
    }
}
