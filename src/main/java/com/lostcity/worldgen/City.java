package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.assets.AssetRegistries;
import com.lostcity.assets.PredefinedBuilding;
import com.lostcity.assets.PredefinedCity;
import com.lostcity.assets.PredefinedStreet;
import com.lostcity.config.LostCityConfig;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.ModLogger;
import com.lostcity.util.TimedCache;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Система определения городских чанков.
 * Портировано из City.java (оригинальный Lost Cities).
 * ШАГ 11: TimedCache, ограниченный радиус поиска (как в оригинале).
 */
public class City {

    /** Кэш CityInfo с TTL (как в оригинале). */
    private static final TimedCache<Long, CityInfo> CITY_INFO_CACHE = new TimedCache<>(
        () -> LostCityMod.getConfig() != null ? LostCityMod.getConfig().getCacheCleanupSeconds() : 300);
    
    /** Кэш CityRarityMap по dimension (как в оригинале). Используется когда CITY_CHANCE < 0. */
    private static final Map<String, CityRarityMap> CITY_RARITY_MAP = new java.util.concurrent.ConcurrentHashMap<>();
    
    /** Этап 1.3: Кэш для predefined buildings/streets по ChunkPos. */
    private static Map<ChunkPos, PredefinedBuilding> predefinedBuildingMap = null;
    private static Map<ChunkPos, PredefinedStreet> predefinedStreetMap = null;
    private static Map<ChunkPos, PredefinedCity> predefinedCityMap = null;
    
    /** Этап 3.1: Кэш CityStyle по ChunkPos. Оригинал: CITY_STYLE_CACHE. */
    private static final TimedCache<Long, com.lostcity.assets.CityStyle> CITY_STYLE_CACHE = new TimedCache<>(
        () -> LostCityMod.getConfig() != null ? LostCityMod.getConfig().getCacheCleanupSeconds() : 300);

    private static final float CITY_THRESHOLD = 0.2f;
    
    /**
     * Проверить, является ли чанк городским
     * 
     * @param pos Позиция чанка
     * @param config Конфигурация мода
     * @return true если чанк городской
     */
    public static boolean isCity(ChunkPos pos, LostCityConfig config) {
        return getCityFactor(pos, config) > CITY_THRESHOLD;
    }
    
    /**
     * Проверить, является ли чанк городским (с указанным миром)
     * 
     * @param pos Позиция чанка
     * @param config Конфигурация мода
     * @param world Мир для получения высоты и seed (может быть null)
     * @return true если чанк городской
     */
    public static boolean isCity(ChunkPos pos, LostCityConfig config, StructureWorldAccess world) {
        return getCityFactor(pos, config, world) > CITY_THRESHOLD;
    }
    
    /**
     * Вычислить фактор города для чанка (0.0 - 1.0)
     * Чем выше значение, тем больше вероятность что это город
     * 
     * @param pos Позиция чанка
     * @param config Конфигурация мода
     * @return Фактор города (0.0 - нет города, 1.0 - центр города)
     */
    public static float getCityFactor(ChunkPos pos, LostCityConfig config) {
        StructureWorldAccess world = ChunkHeightmap.getCurrentWorld();
        return getCityFactor(pos, config, world);
    }
    
    /**
     * Вычислить фактор города для чанка (0.0 - 1.0) с указанным миром.
     * Этап 1.2: Добавлена полная логика из оригинала (высота, spawn distance, perlin noise).
     * 
     * @param pos Позиция чанка
     * @param config Конфигурация мода
     * @param world Мир для получения высоты и seed (может быть null)
     * @return Фактор города (0.0 - нет города, 1.0 - центр города)
     */
    public static float getCityFactor(ChunkPos pos, LostCityConfig config, StructureWorldAccess world) {
        ProfileConfig profile = config.getActiveProfile();
        double cityChance = profile.getCityChance();
        
        int chunkX = pos.x;
        int chunkZ = pos.z;
        
        // Этап 1.3: Проверка predefined buildings/streets
        if (world != null) {
            PredefinedBuilding predefinedBuilding = getPredefinedBuildingAtTopLeft(world, pos);
            if (predefinedBuilding != null) {
                return 1.0f;
            }
            PredefinedStreet predefinedStreet = getPredefinedStreet(world, pos);
            if (predefinedStreet != null) {
                return 1.0f;
            }
            
            // Проверка соседних чанков для multibuilding
            PredefinedBuilding west = getPredefinedBuildingAtTopLeft(world, new ChunkPos(chunkX - 1, chunkZ));
            if (west != null && west.multi) return 1.0f;
            PredefinedBuilding northWest = getPredefinedBuildingAtTopLeft(world, new ChunkPos(chunkX - 1, chunkZ - 1));
            if (northWest != null && northWest.multi) return 1.0f;
            PredefinedBuilding north = getPredefinedBuildingAtTopLeft(world, new ChunkPos(chunkX, chunkZ - 1));
            if (north != null && north.multi) return 1.0f;
        }

        float factor = 0.0f;
        
        // Если CITY_CHANCE < 0, используем Perlin noise через CityRarityMap
        if (cityChance < 0) {
            if (world != null) {
                String dimension = world.toServerWorld().getRegistryKey().getValue().toString();
                long seed = world.toServerWorld().getSeed();
                CityRarityMap rarityMap = getCityRarityMap(dimension, seed,
                        profile.getCityPerlinScale(), profile.getCityPerlinOffset(), profile.getCityPerlinInnerScale());
                factor = rarityMap.getCityFactor(chunkX, chunkZ);
            } else {
                return 0.0f;
            }
        } else if (cityChance > 0.0) {
            // Обычная генерация: суммируем contributions от всех центров городов в радиусе
            int maxRadiusBlocks = profile.getCityMaxRadius();
            // Оригинал: offset = (CITY_MAXRADIUS + 15) / 16 — радиус в блоках, окно в чанках
            int offset = Math.max(1, (maxRadiusBlocks + 15) / 16);

            for (int dx = -offset; dx <= offset; dx++) {
                for (int dz = -offset; dz <= offset; dz++) {
                    int cx = chunkX + dx;
                    int cz = chunkZ + dz;
                    CityInfo info = getCityInfo(cx, cz, profile);
                    if (!info.isCenter) continue;
                    // Расстояние в блоках (центры чанков), как в оригинале
                    float sqdist = (cx * 16 - (chunkX << 4)) * (cx * 16 - (chunkX << 4))
                        + (cz * 16 - (chunkZ << 4)) * (cz * 16 - (chunkZ << 4));
                    if (sqdist >= info.radius * info.radius) continue;
                    float dist = (float) Math.sqrt(sqdist);
                    float contribution = (info.radius - dist) / info.radius;
                    factor += contribution; // Оригинал: суммируем, не берём max
                }
            }
        } else {
            // cityChance == 0.0
            return 0.0f;
        }
        
        // Этап 1.2: Проверка высоты террейна (CITY_MINHEIGHT, CITY_MAXHEIGHT)
        if (factor > 0.0001f && world != null) {
            int height = ChunkHeightmap.getHeight(world, chunkX, chunkZ);
            if (height < profile.getCityMinHeight()) {
                return 0.0f;
            }
            if (height > profile.getCityMaxHeight()) {
                return 0.0f;
            }
            
            // Этап 2.2: Проверка void chunks для floating профиля
            if (profile.isFloating() && profile.getCityAvoidVoid()) {
                if (isVoidChunk(pos, profile, world)) {
                    return 0.0f;
                }
            }
        }
        
        // Этап 1.3: Упрощенный учёт WorldStyle multipliers
        // В оригинале используется WorldStyle.getCityChanceMultiplier() который проверяет биомы
        // Так как полная WorldStyle система отсутствует, используем упрощенную версию
        // TODO: В будущем можно добавить полную поддержку WorldStyle с биомными множителями
        if (factor > 0.0001f && world != null) {
            // Упрощенная версия: всегда возвращаем 1.0f (без множителей по биомам)
            // В оригинале: WorldStyle.getCityChanceMultiplier() проверяет биомы и возвращает множитель
            // Для полной реализации потребуется:
            // 1. Создать класс WorldStyle с поддержкой cityBiomeMultiplier
            // 2. Загружать WorldStyle из JSON
            // 3. Получать биом чанка и проверять его против предикатов
            float multiplier = 1.0f; // Упрощенная версия - всегда 1.0f
            factor *= multiplier;
        }
        
        // Этап 1.2: Поддержка CITY_SPAWN_DISTANCE (масштабирование по расстоянию от спавна)
        if (profile.getCitySpawnDistance2() > 0) {
            // Расстояние от спавна (0, 0) в блоках
            float dist = (float) Math.sqrt((chunkX << 4) * (chunkX << 4) + (chunkZ << 4) * (chunkZ << 4));
            double factorDist;
            if (dist <= profile.getCitySpawnDistance1()) {
                factorDist = profile.getCitySpawnMultiplier1();
            } else if (dist >= profile.getCitySpawnDistance2()) {
                factorDist = profile.getCitySpawnMultiplier2();
            } else {
                float f = (dist - profile.getCitySpawnDistance1()) / (profile.getCitySpawnDistance2() - profile.getCitySpawnDistance1());
                factorDist = profile.getCitySpawnMultiplier1() + f * (profile.getCitySpawnMultiplier2() - profile.getCitySpawnMultiplier1());
            }
            factor *= (float) factorDist;
        }
        
        // Ограничиваем factor в диапазоне [0, 1]
        float finalFactor = Math.min(Math.max(factor, 0), 1);
        return finalFactor;
    }
    
    /**
     * Получить информацию о городе с центром в данном чанке
     * 
     * @param chunkX X координата чанка
     * @param chunkZ Z координата чанка
     * @param profile Профиль конфигурации
     * @return Информация о городе
     */
    private static CityInfo getCityInfo(int chunkX, int chunkZ, ProfileConfig profile) {
        long key = chunkKey(chunkX, chunkZ);
        return CITY_INFO_CACHE.computeIfAbsent(key, k -> {
            boolean isCenter = isCityCenter(chunkX, chunkZ, profile);
            float radius = isCenter ? getCityRadius(chunkX, chunkZ, profile) : 0f;
            return new CityInfo(isCenter, radius);
        });
    }
    
    /**
     * Проверить, является ли чанк центром города
     * 
     * @param chunkX X координата чанка
     * @param chunkZ Z координата чанка
     * @param profile Профиль конфигурации
     * @return true если это центр города
     */
    private static boolean isCityCenter(int chunkX, int chunkZ, ProfileConfig profile) {
        double cityChance = profile.getCityChance();
        
        // Если CITY_CHANCE < 0, центры городов определяются через CityRarityMap (не здесь)
        // Этот метод вызывается только когда CITY_CHANCE >= 0
        if (cityChance < 0) {
            return false;
        }
        
        // Используем координаты чанка как seed для Random
        // Константы взяты из оригинального мода для совместимости
        // Оригинал: chunkZ * 797003437L + chunkX * 295075153L
        Random random = new Random(chunkZ * 797003437L + chunkX * 295075153L);
        
        // Проверяем вероятность
        return random.nextDouble() < cityChance;
    }
    
    /**
     * Получить радиус города с центром в данном чанке
     * 
     * @param chunkX X координата чанка
     * @param chunkZ Z координата чанка
     * @param profile Профиль конфигурации
     * @return Радиус города в чанках
     */
    /** Радиус города в блоках (оригинал: CITY_MIN/MAXRADIUS). */
    private static float getCityRadius(int chunkX, int chunkZ, ProfileConfig profile) {
        Random random = new Random(chunkZ * 100001653L + chunkX * 295075153L + 12345L);
        int minR = profile.getCityMinRadius();
        int maxR = profile.getCityMaxRadius();
        int range = maxR - minR;
        if (range < 1) return minR;
        return minR + random.nextInt(range + 1);
    }
    
    /**
     * Создать ключ для кэша из координат чанка
     */
    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
    
    /**
     * Проверить, является ли чанк void (пустым) для floating профиля. Оригинал: isVoidChunk().
     * Void chunk = высота <= 0 (нет террейна).
     * 
     * @param pos Позиция чанка
     * @param profile Профиль
     * @param world Мир (может быть null)
     * @return true если чанк void
     */
    public static boolean isVoidChunk(ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        if (!profile.isFloating()) {
            return false;
        }
        if (world == null) {
            return false;
        }
        int height = ChunkHeightmap.getHeight(world, pos.x, pos.z);
        return height <= 0;
    }
    
    /**
     * Этап 3.1: Получить CityStyle для чанка. Оригинал: City.getCityStyle().
     * 
     * @param pos Позиция чанка
     * @param config Конфигурация мода
     * @param world Мир (может быть null)
     * @return CityStyle для чанка или null
     */
    public static com.lostcity.assets.CityStyle getCityStyle(ChunkPos pos, LostCityConfig config, StructureWorldAccess world) {
        long key = chunkKey(pos.x, pos.z);
        return CITY_STYLE_CACHE.computeIfAbsent(key, k -> getCityStyleInt(pos, config, world));
    }
    
    /**
     * Этап 3.1: Внутренний метод для получения CityStyle. Оригинал: City.getCityStyleInt().
     */
    private static com.lostcity.assets.CityStyle getCityStyleInt(ChunkPos pos, LostCityConfig config, StructureWorldAccess world) {
        ProfileConfig profile = config.getActiveProfile();
        int chunkX = pos.x;
        int chunkZ = pos.z;
        
        List<Pair<Float, String>> styles = new ArrayList<>();
        Random cityStyleRandom = new Random(
            (world != null ? world.toServerWorld().getSeed() : 0) + chunkZ * 593441843L + chunkX * 217645177L);
        
        if (profile.getCityChance() < 0) {
            // Perlin noise режим
            if (world != null) {
                String dimension = world.toServerWorld().getRegistryKey().getValue().toString();
                long seed = world.toServerWorld().getSeed();
                CityRarityMap rarityMap = getCityRarityMap(dimension, seed,
                        profile.getCityPerlinScale(), profile.getCityPerlinOffset(), profile.getCityPerlinInnerScale());
                float factor = rarityMap.getCityFactor(chunkX, chunkZ);
                
                if (profile.getCityStyleThreshold() >= 0 && factor < profile.getCityStyleThreshold()) {
                    styles.add(new Pair<>(factor, profile.getCityStyleAlternative()));
                } else {
                    styles.add(new Pair<>(factor, getCityStyleForCityCenter(pos, config, world)));
                }
            }
        } else {
            // Обычный режим - ищем центры городов в радиусе
            int maxRadiusBlocks = profile.getCityMaxRadius();
            int offset = Math.max(1, (maxRadiusBlocks + 15) / 16);
            
            for (int cx = chunkX - offset; cx <= chunkX + offset; cx++) {
                for (int cz = chunkZ - offset; cz <= chunkZ + offset; cz++) {
                    ChunkPos c = new ChunkPos(cx, cz);
                    CityInfo info = getCityInfo(cx, cz, profile);
                    if (!info.isCenter) continue;
                    
                    float radius = info.radius;
                    float sqdist = (cx * 16 - (chunkX << 4)) * (cx * 16 - (chunkX << 4)) 
                        + (cz * 16 - (chunkZ << 4)) * (cz * 16 - (chunkZ << 4));
                    if (sqdist >= radius * radius) continue;
                    
                    float dist = (float) Math.sqrt(sqdist);
                    float factor = (radius - dist) / radius;
                    
                    if (profile.getCityStyleThreshold() >= 0 && factor < profile.getCityStyleThreshold()) {
                        styles.add(new Pair<>(factor, profile.getCityStyleAlternative()));
                    } else {
                        styles.add(new Pair<>(factor, getCityStyleForCityCenter(c, config, world)));
                    }
                }
            }
        }
        
        String cityStyleName;
        if (styles.isEmpty()) {
            // Нет центров городов - используем упрощенную версию (без WorldStyle)
            // В оригинале: provider.getWorldStyle().getRandomCityStyle()
            // Для упрощения используем "citystyle_standard" по умолчанию
            cityStyleName = "citystyle_standard";
        } else {
            // Выбираем случайный CityStyle на основе весов
            Pair<Float, String> fromList = getRandomFromList(cityStyleRandom, styles, p -> p.getLeft());
            cityStyleName = fromList != null ? fromList.getRight() : null;
        }
        
        if (cityStyleName == null || cityStyleName.isBlank()) {
            return null;
        }
        
        return AssetRegistries.getCityStyle(cityStyleName);
    }
    
    /**
     * Этап 3.1: Получить CityStyle для центра города. Оригинал: City.getCityStyleForCityCenter().
     */
    private static String getCityStyleForCityCenter(ChunkPos coord, LostCityConfig config, StructureWorldAccess world) {
        // Проверяем predefined city
        if (world != null) {
            PredefinedCity city = getPredefinedCity(world, coord);
            if (city != null && city.cityStyle != null && !city.cityStyle.isBlank()) {
                return city.cityStyle;
            }
        }
        
        // Иначе выбираем случайный CityStyle (упрощенная версия без WorldStyle)
        // В оригинале: provider.getWorldStyle().getRandomCityStyle()
        int chunkX = coord.x;
        int chunkZ = coord.z;
        Random cityStyleForCenterRandom = new Random(
            (world != null ? world.toServerWorld().getSeed() : 0) + chunkZ * 899809363L + chunkX * 256203221L);
        
        // Упрощенная версия: используем "citystyle_standard" по умолчанию
        // TODO: В будущем можно добавить полную поддержку WorldStyle.getRandomCityStyle()
        return "citystyle_standard";
    }
    
    /**
     * Вспомогательный класс Pair для хранения пары значений.
     */
    private static class Pair<L, R> {
        private final L left;
        private final R right;
        
        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
        
        public L getLeft() { return left; }
        public R getRight() { return right; }
    }
    
    /**
     * Выбрать случайный элемент из списка на основе весов. Оригинал: Tools.getRandomFromList().
     */
    private static <T> T getRandomFromList(Random random, List<T> list, java.util.function.Function<T, Float> weightFunction) {
        if (list == null || list.isEmpty()) return null;
        
        float totalWeight = 0;
        for (T item : list) {
            totalWeight += weightFunction.apply(item);
        }
        
        if (totalWeight <= 0) return null;
        
        float r = random.nextFloat() * totalWeight;
        for (T item : list) {
            r -= weightFunction.apply(item);
            if (r <= 0) {
                return item;
            }
        }
        
        return list.get(list.size() - 1);
    }
    
    /**
     * Этап 1.3: Получить предопределённый город в чанке.
     */
    private static PredefinedCity getPredefinedCity(StructureWorldAccess world, ChunkPos coord) {
        calculatePredefinedMaps(world);
        if (predefinedCityMap == null || predefinedCityMap.isEmpty()) {
            return null;
        }
        return predefinedCityMap.get(coord);
    }
    
    public static void clearCache() {
        CITY_INFO_CACHE.clear();
        CITY_RARITY_MAP.clear();
        CITY_STYLE_CACHE.clear();
        predefinedBuildingMap = null;
        predefinedStreetMap = null;
        predefinedCityMap = null;
        ModLogger.info("City cache cleared");
    }
    
    /**
     * Чанк занят predefined building или street. Оригинал: City.isChunkOccupied.
     */
    public static boolean isChunkOccupied(StructureWorldAccess world, ChunkPos pos) {
        if (world == null) return false;
        return getPredefinedBuildingAtTopLeft(world, pos) != null || getPredefinedStreet(world, pos) != null;
    }
    
    /**
     * Этап 1.3: Получить предопределённое здание в top-left чанке.
     * Оригинал: getPredefinedBuildingAtTopLeft() в City.java
     */
    public static PredefinedBuilding getPredefinedBuildingAtTopLeft(StructureWorldAccess world, ChunkPos coord) {
        calculatePredefinedMaps(world);
        if (predefinedBuildingMap == null || predefinedBuildingMap.isEmpty()) {
            return null;
        }
        return predefinedBuildingMap.get(coord);
    }
    
    /**
     * Этап 1.3: Получить предопределённую улицу в чанке.
     * Оригинал: getPredefinedStreet() в City.java
     */
    public static PredefinedStreet getPredefinedStreet(StructureWorldAccess world, ChunkPos coord) {
        calculatePredefinedMaps(world);
        if (predefinedStreetMap == null || predefinedStreetMap.isEmpty()) {
            return null;
        }
        return predefinedStreetMap.get(coord);
    }
    
    /**
     * Этап 1.3: Вычислить карты predefined buildings/streets из всех PredefinedCity.
     * Оригинал: calculateMap() в City.java
     */
    private static void calculatePredefinedMaps(StructureWorldAccess world) {
        if (predefinedBuildingMap != null && predefinedStreetMap != null) {
            return; // Уже вычислено
        }
        
        if (world == null) {
            predefinedBuildingMap = new HashMap<>();
            predefinedStreetMap = new HashMap<>();
            predefinedCityMap = new HashMap<>();
            return;
        }
        
        String dimension = world.toServerWorld().getRegistryKey().getValue().toString();
        
        predefinedBuildingMap = new HashMap<>();
        predefinedStreetMap = new HashMap<>();
        predefinedCityMap = new HashMap<>();
        
        // Загружаем все PredefinedCity и строим карты
        for (PredefinedCity city : AssetRegistries.getAllPredefinedCities()) {
            // Проверяем dimension
            if (!dimension.equals(city.dimension)) {
                continue;
            }
            
            // Добавляем city в карту по центру
            ChunkPos cityCenter = new ChunkPos(city.chunkX, city.chunkZ);
            predefinedCityMap.put(cityCenter, city);
            
            // Добавляем все buildings
            for (PredefinedBuilding building : city.getPredefinedBuildings()) {
                ChunkPos buildingPos = new ChunkPos(
                    city.chunkX + building.relChunkX,
                    city.chunkZ + building.relChunkZ
                );
                predefinedBuildingMap.put(buildingPos, building);
            }
            
            // Добавляем все streets
            for (PredefinedStreet street : city.getPredefinedStreets()) {
                ChunkPos streetPos = new ChunkPos(
                    city.chunkX + street.relChunkX,
                    city.chunkZ + street.relChunkZ
                );
                predefinedStreetMap.put(streetPos, street);
            }
        }
    }
    
    /**
     * Получить CityRarityMap для dimension (кэшируется).
     * Используется когда CITY_CHANCE < 0 для Perlin noise генерации.
     * Оригинал: getCityRarityMap() в City.java
     */
    public static CityRarityMap getCityRarityMap(String dimension, long seed, double scale, double offset, double innerScale) {
        String key = dimension + "_" + seed;
        return CITY_RARITY_MAP.computeIfAbsent(key, k -> new CityRarityMap(seed, scale, offset, innerScale));
    }
    
    /**
     * Информация о городе
     */
    private static class CityInfo {
        final boolean isCenter;  // Является ли чанк центром города
        final float radius;      // Радиус города в чанках
        
        CityInfo(boolean isCenter, float radius) {
            this.isCenter = isCenter;
            this.radius = radius;
        }
    }
}
