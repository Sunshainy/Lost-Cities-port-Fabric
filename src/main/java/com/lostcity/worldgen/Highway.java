package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.config.LostCityConfig;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.PerlinNoiseGenerator14;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.HashMap;
import java.util.Map;

/**
 * Магистрали (highways). Оригинал: mcjty.lostcities.worldgen.lost.Highway.
 * Октавный Simplex-шум (PerlinNoiseGenerator14) + маска по чанкам; уровень от city level конечных городов.
 */
public final class Highway {

    private static final Map<Long, Integer> X_LEVEL_CACHE = new HashMap<>();
    private static final Map<Long, Integer> Z_LEVEL_CACHE = new HashMap<>();

    private static PerlinNoiseGenerator14 perlinX;
    private static PerlinNoiseGenerator14 perlinZ;
    private static long lastSeed = Long.MIN_VALUE;

    private static void makePerlin(long seed) {
        if (perlinX == null || lastSeed != seed) {
            lastSeed = seed;
            perlinX = new PerlinNoiseGenerator14(seed, 4);
            perlinZ = new PerlinNoiseGenerator14(seed, 4);
        }
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Как в оригинале: perlinX.getValue(cx/MAIN, cz/SECONDARY). makePerlin(seed) вызывается в getHighwayLevel. */
    private static double noiseX(int chunkX, int chunkZ, ProfileConfig profile) {
        double sx = chunkX / profile.getHighwayMainPerlinScale();
        double sz = chunkZ / profile.getHighwaySecondaryPerlinScale();
        return perlinX.getValue(sx, sz);
    }

    /** Как в оригинале: perlinZ.getValue(cx/SECONDARY, cz/MAIN). */
    private static double noiseZ(int chunkX, int chunkZ, ProfileConfig profile) {
        double sx = chunkX / profile.getHighwaySecondaryPerlinScale();
        double sz = chunkZ / profile.getHighwayMainPerlinScale();
        return perlinZ.getValue(sx, sz);
    }

    private static boolean hasXHighway(int chunkX, int chunkZ, ProfileConfig profile) {
        return noiseX(chunkX, chunkZ, profile) > profile.getHighwayPerlinFactor();
    }

    private static boolean hasZHighway(int chunkX, int chunkZ, ProfileConfig profile) {
        return noiseZ(chunkX, chunkZ, profile) > profile.getHighwayPerlinFactor();
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
        Map<Long, Integer> cache = useX ? X_LEVEL_CACHE : Z_LEVEL_CACHE;
        long k = key(pos.x, pos.z);
        if (cache.containsKey(k)) return cache.get(k);

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
            cache.put(k, -1);
            return -1;
        }

        StructureWorldAccess w = world != null ? world : ChunkHeightmap.getCurrentWorld();
        if (w == null) {
            cache.put(k, -1);
            return -1;
        }
        long seed = w.toServerWorld().getSeed();
        makePerlin(seed);

        // Оригинал: ищем lower/higher только если текущий чанк входит в highway (perlin > factor)
        boolean hasCurrent = useX ? hasXHighway(pos.x, pos.z, profile) : hasZHighway(pos.x, pos.z, profile);
        if (!hasCurrent) {
            cache.put(k, -1);
            return -1;
        }

        int lower = useX ? pos.x : pos.z;
        while (lower >= -10000) {
            int cx = useX ? lower : pos.x;
            int cz = useX ? pos.z : lower;
            boolean has = useX ? hasXHighway(cx, cz, profile) : hasZHighway(cx, cz, profile);
            if (!has) break;
            lower--;
        }
        lower++;

        int higher = useX ? pos.x : pos.z;
        while (higher <= 10000) {
            int cx = useX ? higher : pos.x;
            int cz = useX ? pos.z : higher;
            boolean has = useX ? hasXHighway(cx, cz, profile) : hasZHighway(cx, cz, profile);
            if (!has) break;
            higher++;
        }
        higher--;

        if (higher - lower < 5) {
            cache.put(k, -1);
            return -1;
        }

        ChunkPos lowerPos = useX ? new ChunkPos(lower, pos.z) : new ChunkPos(pos.x, lower);
        ChunkPos higherPos = useX ? new ChunkPos(higher, pos.z) : new ChunkPos(pos.x, higher);
        // Этап 1.2: Передаём world для проверки высоты (может быть null)
        boolean cityLower = City.isCity(lowerPos, config, world);
        boolean cityHigher = City.isCity(higherPos, config, world);
        boolean valid = profile.getHighwayRequiresTwoCities() ? (cityLower && cityHigher) : (cityLower || cityHigher);
        if (!valid) {
            cache.put(k, -1);
            return -1;
        }

        int levelLower = ChunkHeightmap.getCityLevel(lowerPos, profile, world);
        int levelHigher = ChunkHeightmap.getCityLevel(higherPos, profile, world);
        int level = switch (profile.getHighwayLevelFromCitiesMode()) {
            case 0 -> levelLower;
            case 1 -> Math.min(levelLower, levelHigher);
            case 2 -> Math.max(levelLower, levelHigher);
            case 3 -> (levelLower + levelHigher) / 2;
            default -> levelLower;
        };

        for (int i = lower; i <= higher; i++) {
            long key = useX ? key(i, pos.z) : key(pos.x, i);
            cache.put(key, level);
        }
        return level;
    }

    public static void cleanCache() {
        X_LEVEL_CACHE.clear();
        Z_LEVEL_CACHE.clear();
        perlinX = null;
        perlinZ = null;
        lastSeed = Long.MIN_VALUE;
    }
}
