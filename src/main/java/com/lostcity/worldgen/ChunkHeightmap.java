package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.TimedCache;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;

/**
 * Высота чанка для cityLevel. Оригинал: ChunkHeightmap + getLevelBasedOnHeight.
 * Используем OCEAN_FLOOR_WG в центре чанка (как в оригинале).
 */
public final class ChunkHeightmap {

    private static final int DEFAULT_GROUND = 71;

    private static final TimedCache<Long, Integer> HEIGHT_CACHE = new TimedCache<>(
        () -> LostCityMod.getConfig() != null ? LostCityMod.getConfig().getCacheCleanupSeconds() : 300);
    private static final TimedCache<Long, Integer> CITY_LEVEL_CACHE = new TimedCache<>(
        () -> LostCityMod.getConfig() != null ? LostCityMod.getConfig().getCacheCleanupSeconds() : 300);

    /** Мир текущей генерации (для getCityLevel при вызове из getXmin и т.д.). */
    private static final ThreadLocal<StructureWorldAccess> CURRENT_WORLD = new ThreadLocal<>();

    public static void setCurrentWorld(StructureWorldAccess world) {
        CURRENT_WORLD.set(world);
    }

    public static void clearCurrentWorld() {
        CURRENT_WORLD.remove();
    }

    public static StructureWorldAccess getCurrentWorld() {
        return CURRENT_WORLD.get();
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Высота в центре чанка (OCEAN_FLOOR_WG). Кэшируется.
     */
    public static int getHeight(StructureWorldAccess world, int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Integer cached = HEIGHT_CACHE.get(key);
        if (cached != null) return cached;

        int cx = (chunkX << 4) + 8;
        int cz = (chunkZ << 4) + 8;
        int h;
        try {
            h = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, cx, cz);
        } catch (Exception e) {
            h = DEFAULT_GROUND;
        }
        HEIGHT_CACHE.put(key, h);
        return h;
    }

    /**
     * City level 0..8 по высоте и порогам профиля. Кэшируется.
     * Поддерживает разные типы профилей: space, floating, cavern, normal.
     * world может быть null — тогда берётся из CURRENT_WORLD (в контексте generate).
     * Оригинал: BuildingInfo.getCityLevel()
     */
    public static int getCityLevel(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        long key = chunkKey(pos.x, pos.z);
        Integer cached = CITY_LEVEL_CACHE.get(key);
        if (cached != null) return cached;

        StructureWorldAccess w = world != null ? world : CURRENT_WORLD.get();
        if (w == null) {
            CITY_LEVEL_CACHE.put(key, 0);
            return 0;
        }
        
        int result;
        // Этап 2.2: Поддержка всех профилей
        if (profile.isSpace()) {
            // Для space профиля используем упрощенную версию без CitySphere
            // (CitySphere функционал исключен по требованию)
            result = getCityLevelNormal(pos, profile, w);
        } else if (profile.isFloating()) {
            result = getCityLevelFloating(pos, profile, w);
        } else if (profile.isCavern()) {
            result = getCityLevelCavern(pos, profile, w);
        } else {
            result = getCityLevelNormal(pos, profile, w);
        }
        
        CITY_LEVEL_CACHE.put(key, result);
        return result;
    }
    
    /**
     * City level для floating профиля. Оригинал: getCityLevelFloating().
     * Просто использует высоту и getLevelBasedOnHeight().
     */
    private static int getCityLevelFloating(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        int h = getHeight(world, pos.x, pos.z);
        return profile.getLevelBasedOnHeight(h);
    }
    
    /**
     * City level для cavern профиля. Оригинал: getCityLevelCavern().
     * Пока использует ту же логику, что и floating.
     */
    private static int getCityLevelCavern(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        // @todo for now - как в оригинале
        return getCityLevelFloating(pos, profile, world);
    }
    
    /**
     * City level для normal профиля. Оригинал: getCityLevelNormal().
     * Поддерживает USE_AVG_HEIGHTMAP для усреднения высоты по соседним городским чанкам.
     */
    private static int getCityLevelNormal(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        int height = getHeight(world, pos.x, pos.z);
        
        // Этап 2.2: Поддержка USE_AVG_HEIGHTMAP
        if (profile.getUseAvgHeightmap() && profile.getHeightSampleSize() > 2) {
            int sampleSize = profile.getHeightSampleSize();
            int constX = pos.x < 0 ? -1 : 1;
            int constZ = pos.z < 0 ? -1 : 1;
            int chunkBaseX = (pos.x / sampleSize) * sampleSize + (sampleSize / 2 * constX);
            int chunkBaseZ = (pos.z / sampleSize) * sampleSize + (sampleSize / 2 * constZ);
            int chunkLeft = ((pos.x / sampleSize) - 1) * sampleSize + (sampleSize / 2 * constX);
            int chunkRight = ((pos.x / sampleSize) + 1) * sampleSize + (sampleSize / 2 * constX);
            int chunkUp = ((pos.z / sampleSize) - 1) * sampleSize + (sampleSize / 2 * constZ);
            int chunkDown = ((pos.z / sampleSize) + 1) * sampleSize + (sampleSize / 2 * constZ);
            
            // Получаем конфигурацию для проверки isCity
            com.lostcity.config.LostCityConfig config = com.lostcity.LostCityMod.getConfig();
            if (config == null) {
                return profile.getLevelBasedOnHeight(height);
            }
            
            int avgHeightmap = height;
            int counter = 1;
            
            // Проверяем соседние чанки и усредняем высоту только для городских чанков
            ChunkPos left = new ChunkPos(chunkLeft, chunkBaseZ);
            if (isCityRaw(left, config, world)) {
                avgHeightmap += getHeight(world, chunkLeft, chunkBaseZ);
                counter++;
            }
            
            ChunkPos right = new ChunkPos(chunkRight, chunkBaseZ);
            if (isCityRaw(right, config, world)) {
                avgHeightmap += getHeight(world, chunkRight, chunkBaseZ);
                counter++;
            }
            
            ChunkPos up = new ChunkPos(chunkBaseX, chunkUp);
            if (isCityRaw(up, config, world)) {
                avgHeightmap += getHeight(world, chunkBaseX, chunkUp);
                counter++;
            }
            
            ChunkPos down = new ChunkPos(chunkBaseX, chunkDown);
            if (isCityRaw(down, config, world)) {
                avgHeightmap += getHeight(world, chunkBaseX, chunkDown);
                counter++;
            }
            
            avgHeightmap /= counter;
            return profile.getLevelBasedOnHeight(avgHeightmap);
        }
        
        return profile.getLevelBasedOnHeight(height);
    }
    
    /**
     * Проверить, является ли чанк городским без кеширования. Оригинал: isCityRaw().
     * Используется для USE_AVG_HEIGHTMAP.
     */
    private static boolean isCityRaw(ChunkPos pos, com.lostcity.config.LostCityConfig config, StructureWorldAccess world) {
        if (config == null) return false;
        // Используем City.getCityFactor() для проверки (как в оригинале)
        float cityFactor = com.lostcity.worldgen.City.getCityFactor(pos, config, world);
        float threshold = 0.2f; // CITY_THRESHOLD
        return cityFactor > threshold;
    }

    public static void cleanCache() {
        HEIGHT_CACHE.clear();
        CITY_LEVEL_CACHE.clear();
    }
}

