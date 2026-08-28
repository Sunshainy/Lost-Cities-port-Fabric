package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.assets.AssetRegistries;
import com.lostcity.assets.Building;
import com.lostcity.assets.BuildingPart;
import com.lostcity.assets.PredefinedBuilding;
import com.lostcity.assets.PredefinedStreet;
import com.lostcity.config.LostCityConfig;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.ModLogger;
import com.lostcity.util.TimedCache;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.lostcity.util.QualityRandom;

/**
 * Информация о чанке: городской ли, есть ли здание, тип здания, этажи, улицы, связи.
 * Портирован из BuildingInfo (оригинальный Forge мод).
 * ШАГ 9: добавлены streetType, соседи, connectionAtX/Z, hasRoadConnection.
 */
public class BuildingInfo {

    public enum StreetType { NORMAL, FULL, PARK }

    /** Направление уличной лестницы (в сторону соседа, у которого cityLevel на 1 выше). */
    public enum StairDirection { XMIN, XMAX, ZMIN, ZMAX }

    public final ChunkPos chunkPos;
    public final LostCityConfig config;
    public final boolean isCity;
    public final boolean hasBuilding;
    public final String buildingType;
    public final int floors;
    public final int cellars;
    public final int groundLevel;
    /** Уровень города (0 = основной). Оригинал: cityLevel. */
    public final int cityLevel;
    /** Тип улицы для чанков без здания. */
    public final StreetType streetType;
    /** Связь по X (запад) на каждом этаже: true = прилегает к соседу, нет стены. */
    public final boolean[] connectionAtX;
    /** Связь по Z (север) на каждом этаже. */
    public final boolean[] connectionAtZ;
    /** Часть-фасад (front) для улицы: building_front1/2/3. Только при hasBuilding. */
    public final String frontType;
    /** Позиция в мультиздании. Оригинал: multiPos. */
    public final MultiPos multiBuildingPos;
    /** Мульти-здание (если чанк часть мульти-здания). Оригинал: multiBuilding. */
    public final com.lostcity.assets.MultiBuilding multiBuilding;
    /** Часть для уличной лестницы (только для street). Оригинал: stairType. */
    public final String stairType;
    /** Приоритет лестницы при конкуренции. Оригинал: stairPriority. */
    public final float stairPriority;
    /** Уровень X-магистрали: -1 нет, 0/1 уровень. Оригинал: highwayXLevel. */
    public final int highwayXLevel;
    /** Уровень Z-магистрали. Оригинал: highwayZLevel. */
    public final int highwayZLevel;
    
    // === МОСТИКИ (Bridges) ===
    /** Флаг: может ли быть X-мост в этом чанке. Оригинал: xBridge. */
    public final boolean xBridge;
    /** Флаг: может ли быть Z-мост в этом чанке. Оригинал: zBridge. */
    public final boolean zBridge;
    /** A boolean indicating that this chunk is a candidate for holding a corridor (no guarantee) */
    public final boolean xRailCorridor;
    /** A boolean indicating that this chunk is a candidate for holding a corridor (no guarantee) */
    public final boolean zRailCorridor;
    /** Тип части X-моста (если есть). Оригинал: xBridgeType. */
    private BuildingPart xBridgeType;
    /** Флаг: рассчитан ли xBridgeType. Оригинал: xBridgeTypeCalculated. */
    private boolean xBridgeTypeCalculated = false;
    /** Тип части Z-моста (если есть). Оригинал: zBridgeType. */
    private BuildingPart zBridgeType;
    /** Флаг: рассчитан ли zBridgeType. Оригинал: zBridgeTypeCalculated. */
    private boolean zBridgeTypeCalculated = false;
    /** Часть моста для этого чанка (bridge_open/bridge_covered). Оригинал: bridgeType. */
    public final BuildingPart bridgeType;
    
    // === ПАЛИТРЫ (для multibuilding) ===
    /** Выбранная палитра кирпичей (для единообразия в multibuilding). */
    public final String selectedBricksPalette;
    /** Выбранная палитра стекла (для единообразия в multibuilding). */
    public final String selectedGlassPalette;
    /** Выбранная палитра боковых сторон стекла (для единообразия в multibuilding). */
    public final String selectedGlassSidePalette;
    
    // === ДВЕРИ И ЛУТ ===
    /** Тип двери для этого здания (случайный выбор). Оригинал: doorBlock. */
    public final net.minecraft.block.Block doorBlock;
    /** Флаг отсутствия лута и спавнеров для этого здания. Оригинал: noLoot. */
    public final boolean noLoot;
    /** Список отложенных задач генерации для этого здания. */
    private final List<Runnable> postTodos = new java.util.ArrayList<>();
    
    // === TERRAIN CORRECTION (Этап 2.1) ===
    /** Кеш для desiredMaxHeightL1. Оригинал: desiredMaxHeight1. */
    private MinMax desiredMaxHeight1 = null;
    /** Кеш для desiredMaxHeightL2. Оригинал: desiredTerrainCorrectionHeights. */
    private MinMax desiredTerrainCorrectionHeights = null;

    public static final int FLOOR_HEIGHT = 6;
    private static final String[] FRONT_PARTS = { "lostcities:building_front1", "lostcities:building_front2", "lostcities:building_front3" };
    private static final String[] STAIR_PARTS = { "lostcities:stairs1", "lostcities:stairs2", "lostcities:stairsnormal", "lostcities:stairsbig" };

    private static final TimedCache<ChunkPos, BuildingInfo> CACHE = new TimedCache<>(
        () -> LostCityMod.getConfig() != null ? LostCityMod.getConfig().getCacheCleanupSeconds() : 300);
    private static final AtomicInteger callCount = new AtomicInteger(0);
    
    /** Защита от рекурсии: отслеживаем чанки, которые сейчас вычисляются. */
    private static final ThreadLocal<java.util.Set<ChunkPos>> IN_PROGRESS = ThreadLocal.withInitial(() -> new java.util.HashSet<>());

    public static void cleanCache() {
        CACHE.clear();
        ChunkHeightmap.cleanCache();
        Highway.cleanCache();
        MultiChunk.cleanCache();
        ModLogger.info("BuildingInfo cache cleared");
    }

    /** Город без кеша. Оригинал: isCityRaw. */
    public static boolean isCityRaw(ChunkPos coord, LostCityConfig config, ProfileConfig profile, StructureWorldAccess world) {
        float threshold = profile != null ? profile.getCityThreshold() : 0.2f;
        return City.getCityFactor(coord, config, world) > threshold;
    }

    /** Есть магистраль в чанке. Оригинал: hasHighway. */
    public static boolean hasHighway(ChunkPos coord, LostCityConfig config, ProfileConfig profile, StructureWorldAccess world) {
        return Highway.getXHighwayLevel(coord, profile, world) >= 0 || Highway.getZHighwayLevel(coord, profile, world) >= 0;
    }

    private BuildingInfo(ChunkPos chunkPos, LostCityConfig config,
                         boolean isCity, boolean hasBuilding, String buildingType,
                         int floors, int cellars, int groundLevel, int cityLevel,
                         StreetType streetType, boolean[] connectionAtX, boolean[] connectionAtZ,
                         String frontType, MultiPos multiBuildingPos, com.lostcity.assets.MultiBuilding multiBuilding,
                         String stairType, float stairPriority,
                         int highwayXLevel, int highwayZLevel,
                         boolean xBridge, boolean zBridge, BuildingPart bridgeType,
                         boolean xRailCorridor, boolean zRailCorridor,
                         String selectedBricksPalette, String selectedGlassPalette, String selectedGlassSidePalette,
                         net.minecraft.block.Block doorBlock) {
        this.chunkPos = chunkPos;
        this.config = config;
        this.isCity = isCity;
        this.hasBuilding = hasBuilding;
        this.buildingType = buildingType;
        this.floors = floors;
        this.cellars = cellars;
        this.groundLevel = groundLevel;
        this.cityLevel = cityLevel;
        this.streetType = streetType;
        this.connectionAtX = connectionAtX;
        this.connectionAtZ = connectionAtZ;
        this.frontType = frontType;
        this.multiBuildingPos = multiBuildingPos != null ? multiBuildingPos : MultiPos.SINGLE;
        this.multiBuilding = multiBuilding;
        this.stairType = stairType;
        this.stairPriority = stairPriority;
        this.highwayXLevel = highwayXLevel;
        this.highwayZLevel = highwayZLevel;
        this.xBridge = xBridge;
        this.zBridge = zBridge;
        this.xRailCorridor = xRailCorridor;
        this.zRailCorridor = zRailCorridor;
        this.bridgeType = bridgeType;
        this.selectedBricksPalette = selectedBricksPalette;
        this.selectedGlassPalette = selectedGlassPalette;
        this.selectedGlassSidePalette = selectedGlassSidePalette;
        this.doorBlock = doorBlock;

        ProfileConfig profile = config.getActiveProfile();
        java.util.Random rand = new com.lostcity.util.QualityRandom(chunkPos.x * 2570174657L + chunkPos.z * 101754695981L);
        this.noLoot = profile.getBuildingWithoutLootChance() > 0 && rand.nextFloat() < profile.getBuildingWithoutLootChance();
    }

    public void addPostTodo(Runnable task) {
        if (task != null) {
            this.postTodos.add(task);
        }
    }

    public void executePostTodos() {
        for (Runnable task : postTodos) {
            try {
                task.run();
            } catch (Exception e) {
                ModLogger.error("Error executing postTodo in BuildingInfo ({}, {}): {}", chunkPos.x, chunkPos.z, e.getMessage());
            }
        }
        postTodos.clear();
    }
    
    /**
     * Получить случайный тип двери. Оригинал: getRandomDoor().
     */
    private static net.minecraft.block.Block getRandomDoor(Random rand) {
        return switch (rand.nextInt(7)) {
            case 0 -> net.minecraft.block.Blocks.BIRCH_DOOR;
            case 1 -> net.minecraft.block.Blocks.ACACIA_DOOR;
            case 2 -> net.minecraft.block.Blocks.DARK_OAK_DOOR;
            case 3 -> net.minecraft.block.Blocks.SPRUCE_DOOR;
            case 4 -> net.minecraft.block.Blocks.OAK_DOOR;
            case 5 -> net.minecraft.block.Blocks.JUNGLE_DOOR;
            case 6 -> net.minecraft.block.Blocks.IRON_DOOR;
            default -> net.minecraft.block.Blocks.OAK_DOOR;
        };
    }

    public int getMaxHighwayLevel() {
        return Math.max(highwayXLevel, highwayZLevel);
    }

    public static BuildingInfo get(ChunkPos pos, LostCityConfig config) {
        return get(pos, config, null, false);
    }

    /**
     * @param world мир генерации (для heightmap/cityLevel). Если null, используется ChunkHeightmap.CURRENT_WORLD.
     */
    public static BuildingInfo get(ChunkPos pos, LostCityConfig config, StructureWorldAccess world) {
        return get(pos, config, world, false);
    }
    
    /**
     * @param world мир генерации (для heightmap/cityLevel). Если null, используется ChunkHeightmap.CURRENT_WORLD.
     * @param skipMultiChunk если true, не вычисляет MultiChunk (для избежания рекурсии)
     */
    public static BuildingInfo get(ChunkPos pos, LostCityConfig config, StructureWorldAccess world, boolean skipMultiChunk) {
        // Проверяем кэш сразу
        BuildingInfo cached = CACHE.get(pos);
        if (cached != null) {
            return cached;
        }

        // Защита от рекурсии: проверяем, не вычисляется ли уже этот чанк
        java.util.Set<ChunkPos> inProgress = IN_PROGRESS.get();
        if (inProgress.contains(pos)) {
            // Возвращаем минимальный BuildingInfo чтобы избежать рекурсии
            ProfileConfig profile = config.getActiveProfile();
            int ground = profile.getGroundLevel();
            int cityLevel = ChunkHeightmap.getCityLevel(pos, profile, world);
            return new BuildingInfo(pos, config, false, false, null, 0, 0,
                    ground, cityLevel, StreetType.NORMAL, null, null, null, MultiPos.SINGLE, null, null, 0.0f, -1, -1,
                    false, false, null, false, false, null, null, null, null);
        }
        
        // Помечаем чанк как вычисляемый
        inProgress.add(pos);
        try {
            ModLogger.debug("BuildingInfo.get({}, {}) - calculating (world={}, skipMultiChunk={})", 
                pos.x, pos.z, world != null ? "present" : "null", skipMultiChunk);

            // Этап 1.2: Передаём world для проверки высоты (может быть null - тогда используется ChunkHeightmap.getCurrentWorld())
            boolean isCity = City.isCity(pos, config, world);
            ProfileConfig profile = config.getActiveProfile();
            int ground = profile.getGroundLevel();
            
            // === MULTICHUNK/MULTIBUILDING (определяем раньше для вычисления cityLevel) ===
            // КРИТИЧНО: мульти-здания определяются ТОЛЬКО для городских чанков (как в оригинале)!
            MultiPos multiPos = MultiPos.SINGLE;
            com.lostcity.assets.MultiBuilding multiBuilding = null;
            String actualBuildingType = null;
            
            // ВАЖНО: skipMultiChunk=true предотвращает рекурсию (MultiChunk.canPlaceBuilding -> BuildingInfo.get -> MultiChunk.getOrCreate)
            // КРИТИЧНО: мульти-здания определяются ТОЛЬКО для городских чанков (как в оригинале)!
            if (world != null && !skipMultiChunk && isCity) {
                ModLogger.debug("Calling getMultiBuilding for chunk ({}, {})", pos.x, pos.z);
                MultiChunk.MB mb = MultiChunk.getOrCreate(world, pos, config).getMultiBuilding(pos);
                ModLogger.debug("getMultiBuilding returned: {}", mb != null ? mb.name() : "null");
                if (mb != null) {
                    // Чанк является частью мульти-здания
                    multiBuilding = AssetRegistries.getMultiBuilding(mb.name());
                    if (multiBuilding != null) {
                        multiPos = new MultiPos(mb.offsetX(), mb.offsetZ(), multiBuilding.getDimX(), multiBuilding.getDimZ());
                        actualBuildingType = multiBuilding.getBuilding(mb.offsetX(), mb.offsetZ());
                        ModLogger.debug("Chunk ({}, {}) is part of multibuilding '{}' at offset ({}, {})", 
                            pos.x, pos.z, mb.name(), mb.offsetX(), mb.offsetZ());
                        ModLogger.debug("Selected building part for this chunk: '{}'", actualBuildingType);
                    }
                }
            }
            
            // Этап 3.2: Определяем cityLevel с учётом multibuilding и MULTI_USE_CORNER
            // КРИТИЧНО: для мульти-здания (не top-left) cityLevel должен копироваться из top-left,
            // чтобы все части имели одинаковую высоту!
            int cityLevel;
            if (multiPos.isMulti() && !multiPos.isTopLeft()) {
                // Для мульти-здания (не top-left) копируем cityLevel из top-left
                ChunkPos topLeftPos = new ChunkPos(pos.x - multiPos.x(), pos.z - multiPos.z());
                BuildingInfo topLeft = BuildingInfo.get(topLeftPos, config, world, true);
                if (topLeft != null) {
                    cityLevel = topLeft.cityLevel;
                } else {
                    // Fallback: вычисляем как для top-left
                    if (profile.getMultiUseCorner()) {
                        cityLevel = ChunkHeightmap.getCityLevel(topLeftPos, profile, world);
                    } else {
                        cityLevel = getAverageCityLevel(multiPos, pos, profile, world);
                    }
                }
            } else if (multiPos.isSingle()) {
                // Одиночное здание - используем обычный cityLevel
                cityLevel = ChunkHeightmap.getCityLevel(pos, profile, world);
            } else {
                // Top-left часть multibuilding - вычисляем cityLevel
                if (profile.getMultiUseCorner()) {
                    // Используем cityLevel из top-left угла (это и есть top-left)
                    cityLevel = ChunkHeightmap.getCityLevel(pos, profile, world);
                } else {
                    // Усредняем cityLevel по всем частям multibuilding
                    cityLevel = getAverageCityLevel(multiPos, pos, profile, world);
                }
            }

            // Highway levels нужны для всех чанков (город + негород), чтобы генерировать магистрали везде
            // КРИТИЧНО: для мульти-здания (не top-left) копируем из top-left!
            int hx, hz;
            if (multiPos.isMulti() && !multiPos.isTopLeft()) {
                // Для мульти-здания копируем highway levels из top-left
                ChunkPos topLeftPos = new ChunkPos(pos.x - multiPos.x(), pos.z - multiPos.z());
                BuildingInfo topLeft = BuildingInfo.get(topLeftPos, config, world, true);
                if (topLeft != null) {
                    hx = topLeft.highwayXLevel;
                    hz = topLeft.highwayZLevel;
                } else {
                    // Fallback: вычисляем для текущего чанка
                    hx = Highway.getXHighwayLevel(pos, profile, world);
                    hz = Highway.getZHighwayLevel(pos, profile, world);
                }
            } else {
                // Для одиночного здания или top-left части мульти-здания вычисляем как обычно
                hx = Highway.getXHighwayLevel(pos, profile, world);
                hz = Highway.getZHighwayLevel(pos, profile, world);
            }

            // Мосты: только в негородских чанках. Оригинал: !isCity → xBridge/zBridge по BRIDGE_CHANCE
            boolean xBridge = false, zBridge = false;
            BuildingPart bridgePart = null;
            if (!isCity) {
                Random bridgeRand = new Random(pos.x * 987654323L + pos.z * 123456789L);
                xBridge = bridgeRand.nextFloat() < profile.getBridgeChance();
                zBridge = bridgeRand.nextFloat() < profile.getBridgeChance();
                if (xBridge || zBridge) {
                    String bridgeName = bridgeRand.nextFloat() < 0.5f ? "lostcities:bridge_open" : "lostcities:bridge_covered";
                    bridgePart = AssetRegistries.getPart(bridgeName);
                }
                BuildingInfo info = new BuildingInfo(pos, config, false, false, null, 0, 0,
                        ground, cityLevel, null, null, null, null, MultiPos.SINGLE, null, null, 0f, hx, hz,
                        xBridge, zBridge, bridgePart, false, false, null, null, null, null);
                // КРИТИЧНО: Кэшируем ТОЛЬКО если world != null (иначе multibuilding информация неполная!)
                if (world != null) {
                    CACHE.put(pos, info);
                }
                return info;
            }

            // ВАЖНО: Сид должен совпадать с Forge 1:1 для идентичной генерации
            long seed = world != null ? world.getSeed() : 0;
            QualityRandom rand = new QualityRandom(seed + pos.z * 341873128712L + pos.x * 132897987541L);
            
            // Этап 2.3: Улучшенная логика проверки возможности генерации здания
            // Оригинал: checkBuildingPossibility()
            boolean hasBuilding;
            
            // Проверка predefined building/street (приоритет)
            if (world != null) {
                PredefinedBuilding predefinedBuilding = City.getPredefinedBuildingAtTopLeft(world, pos);
                if (predefinedBuilding != null) {
                    // Predefined building - всегда hasBuilding=true
                    hasBuilding = true;
                } else {
                    PredefinedStreet predefinedStreet = City.getPredefinedStreet(world, pos);
                    if (predefinedStreet != null) {
                        // Predefined street - всегда hasBuilding=false
                        hasBuilding = false;
                    } else {
                        // Обычная проверка
                        hasBuilding = checkBuildingPossibility(pos, config, profile, multiPos, cityLevel, hx, hz, rand);
                    }
                }
            } else {
                // Если world == null, используем упрощенную проверку
                hasBuilding = checkBuildingPossibility(pos, config, profile, multiPos, cityLevel, hx, hz, rand);
            }

            if (!hasBuilding) {
                StreetType st = rand.nextFloat() < 0.1f ? StreetType.PARK : StreetType.values()[rand.nextInt(StreetType.values().length - 1)];
                String stair = STAIR_PARTS[rand.nextInt(STAIR_PARTS.length)];
                float priority = rand.nextFloat();
                BuildingInfo info = new BuildingInfo(pos, config, true, false, null, 0, 0,
                        ground, cityLevel, st, null, null, null, MultiPos.SINGLE, null, stair, priority, hx, hz,
                        false, false, null, false, false, null, null, null, null);
                // КРИТИЧНО: Кэшируем ТОЛЬКО если !skipMultiChunk И world != null
                if (!skipMultiChunk && world != null) {
                    CACHE.put(pos, info);
                }
                return info;
            }

            Map<String, Building> allBuildings = AssetRegistries.getBuildings();
            if (allBuildings.isEmpty()) {
                ModLogger.warn("No buildings loaded! Assets not loaded?");
                float corridorChance = profile.getCorridorChance();
                boolean xrc = rand.nextFloat() < corridorChance;
                boolean zrc = rand.nextFloat() < corridorChance;
                BuildingInfo info = new BuildingInfo(pos, config, true, false, null, 0, 0,
                        ground, cityLevel, null, null, null, null, MultiPos.SINGLE, null, null, 0f, hx, hz,
                        false, false, null, xrc, zrc, null, null, null, null);
                // КРИТИЧНО: Кэшируем ТОЛЬКО если !skipMultiChunk И world != null
                if (!skipMultiChunk && world != null) {
                    CACHE.put(pos, info);
                }
                return info;
            }

            // Если не часть мульти-здания, выбираем случайное одиночное здание
            // КРИТИЧНО: Исключаем части multibuilding (center00, library01, и т.д.)
            if (multiPos.isSingle()) {
                // Этап 1.3: Проверка predefined building (приоритет над случайным выбором)
                if (world != null) {
                    PredefinedBuilding predefinedBuilding = City.getPredefinedBuildingAtTopLeft(world, pos);
                    if (predefinedBuilding != null && predefinedBuilding.building != null) {
                        actualBuildingType = predefinedBuilding.building;
                        ModLogger.debug("Using predefined building '{}' for chunk ({}, {})", actualBuildingType, pos.x, pos.z);
                    }
                }
                
                // Если predefined building не найден, выбираем здание из CityStyle или стандартного пула
                if (actualBuildingType == null) {
                    com.lostcity.assets.CityStyle cs = world != null ? City.getCityStyle(pos, config, world) : null;
                    if (cs != null) {
                        String styleBuilding = cs.getRandomBuilding(rand);
                        if (styleBuilding != null) {
                            if (allBuildings.containsKey(styleBuilding)) {
                                actualBuildingType = styleBuilding;
                            } else if (allBuildings.containsKey("lostcities:" + styleBuilding)) {
                                actualBuildingType = "lostcities:" + styleBuilding;
                            }
                        }
                    }
                    
                    if (actualBuildingType == null || !allBuildings.containsKey(actualBuildingType)) {
                        // Резервный случайный выбор: фильтруем части multibuilding И рассыпанные постройки (cabin, radiotower, oilrig)
                        List<String> ids = new ArrayList<>();
                        for (String buildingId : allBuildings.keySet()) {
                            boolean isScattered = buildingId.contains("cabin") || buildingId.contains("radiotower") || buildingId.contains("oilrig");
                            if (!isMultiBuildingPart(buildingId) && !isScattered) {
                                ids.add(buildingId);
                            }
                        }
                        if (ids.isEmpty()) {
                            ids = new ArrayList<>(allBuildings.keySet());
                        }
                        actualBuildingType = ids.get(rand.nextInt(ids.size()));
                    }
                    ModLogger.debug("Selected single building '{}' for chunk ({}, {})", actualBuildingType, pos.x, pos.z);
                }
            }
            
            // КРИТИЧНО: Проверяем, что actualBuildingType установлен
            if (actualBuildingType == null) {
                ModLogger.error("ERROR: actualBuildingType is NULL for chunk ({}, {})! multiPos={}, multiBuilding={}", 
                    pos.x, pos.z, multiPos, multiBuilding != null ? multiBuilding.getName() : "null");
                // Откат к случайному зданию
                List<String> ids = new ArrayList<>(allBuildings.keySet());
                actualBuildingType = ids.get(rand.nextInt(ids.size()));
                ModLogger.error(">>> Fallback to random building: '{}'", actualBuildingType);
            }

            Building building = AssetRegistries.getBuilding(actualBuildingType);
            
            // Этап 2.3: Проверка prefersLonely для соседей (только для одиночных зданий)
            // Оригинал: проверка после выбора здания, но до финального создания BuildingInfo
            if (hasBuilding && multiPos.isSingle() && building != null && world != null) {
                // Проверяем соседей: если соседнее здание предпочитает быть одиноким, отменяем генерацию
                BuildingInfo west = get(new ChunkPos(pos.x - 1, pos.z), config, world, true);
                if (west != null && west.hasBuilding && west.buildingType != null) {
                    Building westBuilding = AssetRegistries.getBuilding(west.buildingType);
                    if (westBuilding != null && rand.nextFloat() < westBuilding.getPrefersLonely()) {
                        ModLogger.debug("West neighbor prefers lonely, skipping building at ({}, {})", pos.x, pos.z);
                        hasBuilding = false;
                    }
                }
                
                if (hasBuilding) {
                    BuildingInfo east = get(new ChunkPos(pos.x + 1, pos.z), config, world, true);
                    if (east != null && east.hasBuilding && east.buildingType != null) {
                        Building eastBuilding = AssetRegistries.getBuilding(east.buildingType);
                        if (eastBuilding != null && rand.nextFloat() < eastBuilding.getPrefersLonely()) {
                            ModLogger.debug("East neighbor prefers lonely, skipping building at ({}, {})", pos.x, pos.z);
                            hasBuilding = false;
                        }
                    }
                }
                
                if (hasBuilding) {
                    BuildingInfo north = get(new ChunkPos(pos.x, pos.z - 1), config, world, true);
                    if (north != null && north.hasBuilding && north.buildingType != null) {
                        Building northBuilding = AssetRegistries.getBuilding(north.buildingType);
                        if (northBuilding != null && rand.nextFloat() < northBuilding.getPrefersLonely()) {
                            ModLogger.debug("North neighbor prefers lonely, skipping building at ({}, {})", pos.x, pos.z);
                            hasBuilding = false;
                        }
                    }
                }
                
                if (hasBuilding) {
                    BuildingInfo south = get(new ChunkPos(pos.x, pos.z + 1), config, world, true);
                    if (south != null && south.hasBuilding && south.buildingType != null) {
                        Building southBuilding = AssetRegistries.getBuilding(south.buildingType);
                        if (southBuilding != null && rand.nextFloat() < southBuilding.getPrefersLonely()) {
                            ModLogger.debug("South neighbor prefers lonely, skipping building at ({}, {})", pos.x, pos.z);
                            hasBuilding = false;
                        }
                    }
                }
                
                // Если hasBuilding стал false после проверки prefersLonely, возвращаем street chunk
                if (!hasBuilding) {
                    StreetType st = rand.nextFloat() < 0.1f ? StreetType.PARK : StreetType.values()[rand.nextInt(StreetType.values().length - 1)];
                    String stair = STAIR_PARTS[rand.nextInt(STAIR_PARTS.length)];
                    float priority = rand.nextFloat();
                    float corridorChance = profile.getCorridorChance();
                    boolean xrc = rand.nextFloat() < corridorChance;
                    boolean zrc = rand.nextFloat() < corridorChance;
                    BuildingInfo info = new BuildingInfo(pos, config, true, false, null, 0, 0,
                            ground, cityLevel, st, null, null, null, MultiPos.SINGLE, null, stair, priority, hx, hz,
                            false, false, null, xrc, zrc, null, null, null, null);
                    if (!skipMultiChunk && world != null) {
                        CACHE.put(pos, info);
                    }
                    return info;
                }
            }
            
            if (building == null) {
                int count = callCount.getAndIncrement();
                ModLogger.error("Building type '{}' not found! Chunk: ({}, {}), multiPos: {}, multiBuilding: {}", 
                    actualBuildingType, pos.x, pos.z, multiPos, multiBuilding != null ? multiBuilding.getName() : "null");
                
                // Показываем все доступные building
                if (count <= 3) {
                    ModLogger.error("Available buildings: {}", allBuildings.keySet());
                }
                
                float corridorChance = profile.getCorridorChance();
                boolean xrc = rand.nextFloat() < corridorChance;
                boolean zrc = rand.nextFloat() < corridorChance;
                BuildingInfo info = new BuildingInfo(pos, config, true, false, null, 0, 0,
                        ground, cityLevel, null, null, null, null, MultiPos.SINGLE, null, null, 0f, hx, hz,
                        false, false, null, xrc, zrc, null, null, null, null);
                // КРИТИЧНО: Кэшируем ТОЛЬКО если !skipMultiChunk И world != null
                if (!skipMultiChunk && world != null) {
                    CACHE.put(pos, info);
                }
                return info;
            }

            // КРИТИЧНО: для multibuilding (не top-left) сначала пытаемся получить параметры от top-left!
            // Это должно быть ДО вычисления floors и cellars, чтобы они были одинаковыми для всех частей
            String frontType = null;
            boolean[] connectionAtX = null;
            boolean[] connectionAtZ = null;
            String selectedBricksPalette = null;
            String selectedGlassPalette = null;
            String selectedGlassSidePalette = null;
            net.minecraft.block.Block doorBlock = null;
            
            int floors = 0;
            int cellars = 0;
            boolean degradedMultiPart = false;
            boolean copiedFromTopLeft = false;
            
            // Объявляем cityStyle здесь, чтобы она была доступна для вычисления cellars
            com.lostcity.assets.CityStyle cityStyle = null;
            
            if (multiPos.isMulti() && !multiPos.isTopLeft()) {
                ChunkPos topLeftPos = new ChunkPos(pos.x - multiPos.x(), pos.z - multiPos.z());
                // КРИТИЧНО: пытаемся получить top-left БЕЗ skipMultiChunk, чтобы он был полностью вычислен
                // Но если это приведет к рекурсии, используем skipMultiChunk=true
                BuildingInfo topLeft = null;
                if (!skipMultiChunk && world != null) {
                    // Пробуем получить top-left без skipMultiChunk
                    try {
                        topLeft = BuildingInfo.get(topLeftPos, config, world, false);
                    } catch (Exception e) {
                        // Если рекурсия, используем skipMultiChunk=true
                        ModLogger.debug("Recursion detected when getting top-left, using skipMultiChunk=true");
                        topLeft = BuildingInfo.get(topLeftPos, config, world, true);
                    }
                } else {
                    // Если skipMultiChunk=true или world==null, используем skipMultiChunk=true
                    topLeft = BuildingInfo.get(topLeftPos, config, world, true);
                }
                
                if (topLeft != null && topLeft.hasBuilding) {
                    floors = topLeft.floors;
                    cellars = topLeft.cellars;
                    frontType = topLeft.frontType;
                    doorBlock = topLeft.doorBlock;
                    int nMulti = floors + cellars + 1;
                    connectionAtX = new boolean[nMulti];
                    connectionAtZ = new boolean[nMulti];
                    float doorwayMulti = profile.getBuildingDoorwayChance();
                    for (int i = 0; i < nMulti; i++) {
                        ChunkPos westMulti = new ChunkPos(pos.x - 1, pos.z);
                        ChunkPos northMulti = new ChunkPos(pos.x, pos.z - 1);
                        boolean canConnectXMulti = multiPos.x() >= multiPos.w() - 1;
                        boolean canConnectZMulti = multiPos.z() >= multiPos.h() - 1;
                        connectionAtX[i] = canConnectXMulti && City.isCity(westMulti, config, world) && rand.nextFloat() < doorwayMulti;
                        connectionAtZ[i] = canConnectZMulti && City.isCity(northMulti, config, world) && rand.nextFloat() < doorwayMulti;
                    }
                    selectedBricksPalette = topLeft.selectedBricksPalette;
                    selectedGlassPalette = topLeft.selectedGlassPalette;
                    selectedGlassSidePalette = topLeft.selectedGlassSidePalette;
                    copiedFromTopLeft = true;
                    ModLogger.debug("Multibuilding part ({}, {}) from top-left ({}, {}): floors={}, cellars={}, cityLevel={}", 
                        pos.x, pos.z, topLeftPos.x, topLeftPos.z, floors, cellars, cityLevel);
                } else {
                    // Top-left недоступен - вычисляем floors и cellars с теми же параметрами, что и для top-left
                    // Используем детерминированный Random на основе top-left позиции, чтобы все части имели одинаковую высоту
                    ModLogger.warn("Multibuilding part ({}, {}): top-left ({}, {}) unavailable (null={}, hasBuilding={}). Computing floors/cellars with deterministic random.",
                        pos.x, pos.z, topLeftPos.x, topLeftPos.z, topLeft == null, topLeft != null && topLeft.hasBuilding);
                    // Используем детерминированный Random на основе top-left позиции
                    Random topLeftRand = new Random(topLeftPos.x * 341873128712L + topLeftPos.z * 132897987541L);
                    com.lostcity.assets.CityStyle topLeftCityStyle = world != null ? City.getCityStyle(topLeftPos, config, world) : null;
                    
                    // Вычисляем floors и cellars с теми же параметрами, что и для top-left
                    int maxfloors = profile.getBuildingMaxFloors();
                    if (topLeftCityStyle != null && topLeftCityStyle.getMaxFloorCount() != null) {
                        maxfloors = Math.min(maxfloors, topLeftCityStyle.getMaxFloorCount());
                    }
                    int minfloors = profile.getBuildingMinFloors() + 1;
                    if (topLeftCityStyle != null && topLeftCityStyle.getMinFloorCount() != null) {
                        minfloors = Math.max(minfloors, topLeftCityStyle.getMinFloorCount());
                    }
                    floors = Math.max(1, minfloors - 1 + topLeftRand.nextInt(Math.max(1, maxfloors - (minfloors - 1) + 1)));
                    
                    // Вычисляем cellars
                    int maxcellars = cityLevel;
                    int mincellars = 0;
                    if (topLeftCityStyle != null) {
                        if (topLeftCityStyle.getMaxCellarCount() != null) {
                            maxcellars = Math.min(maxcellars, topLeftCityStyle.getMaxCellarCount());
                        }
                        if (topLeftCityStyle.getMinCellarCount() != null) {
                            mincellars = Math.max(mincellars, topLeftCityStyle.getMinCellarCount());
                        }
                    }
                    int maxHighwayLevel = Math.max(hx, hz);
                    if (maxHighwayLevel >= 0) {
                        int maxCellarsAllowed = cityLevel - maxHighwayLevel - 1;
                        maxcellars = Math.min(maxcellars, maxCellarsAllowed);
                    }
                    if (maxcellars > 0 && maxcellars >= mincellars) {
                        cellars = mincellars + topLeftRand.nextInt(maxcellars - mincellars + 1);
                    } else {
                        cellars = 0;
                    }
                    
                    frontType = FRONT_PARTS[topLeftRand.nextInt(FRONT_PARTS.length)];
                    doorBlock = getRandomDoor(topLeftRand);
                    // connectionAtX и connectionAtZ будут вычислены ниже
                    selectedBricksPalette = "lostcities:bricks_standard";
                    selectedGlassPalette = "lostcities:glass_full";
                    selectedGlassSidePalette = "lostcities:glass_side_variant_glass";
                }
            } else {
                // Это одиночное здание или top-left часть multibuilding — вычисляем параметры
                
                // Если не скопировали из top-left, вычисляем floors и cellars
                if (!copiedFromTopLeft) {
                    // Этап 3.1: Вычисляем floors и cellars с учётом CityStyle (как в оригинале)
                    cityStyle = world != null ? City.getCityStyle(pos, config, world) : null;
                    
                    // Вычисляем maxfloors с учётом требований здания, CityStyle и профиля
                    int maxfloors = profile.getBuildingMaxFloors();
                    if (building != null && building.getMaxFloors() != -1) {
                        maxfloors = building.getMaxFloors();
                    } else if (cityStyle != null && cityStyle.getMaxFloorCount() != null) {
                        maxfloors = Math.min(maxfloors, cityStyle.getMaxFloorCount());
                    }
                    
                    // Вычисляем minfloors с учётом требований здания, CityStyle и профиля
                    int minfloors = profile.getBuildingMinFloors() + 1; // +1 потому что не считаем верхний этаж
                    if (building != null && building.getMinFloors() != -1) {
                        minfloors = building.getMinFloors();
                    } else if (cityStyle != null && cityStyle.getMinFloorCount() != null) {
                        minfloors = Math.max(minfloors, cityStyle.getMinFloorCount());
                    }
                    
                    floors = minfloors >= maxfloors ? minfloors : (minfloors + rand.nextInt(maxfloors - minfloors + 1));
                    cellars = 0; // cellars будут вычислены ниже
                }
                
                frontType = FRONT_PARTS[rand.nextInt(FRONT_PARTS.length)];
                doorBlock = getRandomDoor(rand); // Генерируем случайную дверь
                connectionAtX = null;
                connectionAtZ = null;
            
                // Этап 3.1: Выбираем палитры через CityStyle -> Style.getRandomPalette()
                if (hasBuilding && world != null) {
                    // Используем уже полученный cityStyle, если он есть
                    // Если нет, получаем новый
                    if (cityStyle == null) {
                        cityStyle = City.getCityStyle(pos, config, world);
                    }
                    if (cityStyle != null && cityStyle.getStyle() != null) {
                    com.lostcity.assets.Style style = AssetRegistries.getStyle(cityStyle.getStyle());
                    if (style != null) {
                        // Получаем случайную палитру из Style (объединяет несколько палитр)
                        com.lostcity.assets.Palette randomPalette = style.getRandomPalette(world, rand);
                        if (randomPalette != null) {
                            // Извлекаем имена палитр из объединенной палитры
                            // В оригинале палитры объединяются в одну, но нам нужно сохранить имена для multibuilding
                            // Для упрощения, используем упрощенную логику: сохраняем имена из Style
                            // В оригинале палитры выбираются из Style.getRandomPalette() который объединяет несколько
                            // Для сохранения имен, нужно извлечь их из Style
                            // Упрощенная версия: используем стандартные имена палитр
                            // TODO: В будущем можно улучшить извлечение имен палитр из Style
                            selectedBricksPalette = "lostcities:bricks_standard"; // Упрощенная версия
                            selectedGlassPalette = "lostcities:glass_full"; // Упрощенная версия
                            selectedGlassSidePalette = "lostcities:glass_side_variant_glass"; // Упрощенная версия
                            
                            ModLogger.debug("Top-left or single building ({}, {}) using CityStyle '{}' -> Style '{}', palettes: [{}, {}, {}]",
                                pos.x, pos.z, cityStyle.getId(), cityStyle.getStyle(), 
                                selectedBricksPalette, selectedGlassPalette, selectedGlassSidePalette);
                        }
                    }
                }
                
                // Если CityStyle не найден, используем упрощенный случайный выбор
                if (selectedBricksPalette == null) {
                    String[] bricksPalettes = {
                        "lostcities:bricks_standard", "lostcities:bricks_cyan", 
                        "lostcities:bricks_gray", "lostcities:bricks_silver"
                    };
                    String[] glassPalettes = {
                        "lostcities:glass_full", "lostcities:glass_pane",
                        "lostcities:glass_full_white", "lostcities:glass_pane_white",
                        "lostcities:glass_full_blue", "lostcities:glass_pane_blue",
                        "lostcities:glass_full_light_blue", "lostcities:glass_pane_light_blue",
                        "lostcities:glass_full_gray", "lostcities:glass_pane_gray"
                    };
                    String[] glassSidePalettes = {
                        "lostcities:glass_side_variant_glass", "lostcities:glass_side_variant_bricks",
                        "lostcities:glass_side_variant_quartz"
                    };
                    
                    selectedBricksPalette = bricksPalettes[rand.nextInt(bricksPalettes.length)];
                    selectedGlassPalette = glassPalettes[rand.nextInt(glassPalettes.length)];
                    selectedGlassSidePalette = glassSidePalettes[rand.nextInt(glassSidePalettes.length)];
                    
                    ModLogger.debug("Top-left or single building ({}, {}) selected palettes (fallback): [{}, {}, {}], doorBlock={}",
                        pos.x, pos.z, selectedBricksPalette, selectedGlassPalette, selectedGlassSidePalette, doorBlock);
                }
            }
            }
            
            // Этап 3.1: Вычисляем cellars с учётом CityStyle (как в оригинале)
            // КРИТИЧНО: для мульти-зданий (не top-left) floors и cellars уже скопированы из top-left,
            // НЕ перезаписываем их!
            if (!degradedMultiPart && (multiPos.isSingle() || multiPos.isTopLeft())) {
                // Только для одиночных зданий или top-left части мульти-здания вычисляем cellars
                // Оригинал: getMaxcellars() и getMincellars() с учётом CityStyle
                
                // Если cityStyle еще не получен, получаем его сейчас
                if (cityStyle == null && world != null) {
                    cityStyle = City.getCityStyle(pos, config, world);
                }
                
                int maxcellars = profile.getBuildingMaxCellars() + cityLevel;
                int mincellars = profile.getBuildingMinCellars();
                
                if (building != null && building.getMaxCellars() != -1) {
                    maxcellars = Math.min(maxcellars, building.getMaxCellars());
                }
                
                if (cityStyle != null) {
                    if (cityStyle.getMaxCellarCount() != null) {
                        maxcellars = Math.min(maxcellars, cityStyle.getMaxCellarCount());
                    }
                    if (cityStyle.getMinCellarCount() != null) {
                        mincellars = Math.max(mincellars, cityStyle.getMinCellarCount());
                    }
                }
                
                // Ограничение cellars если здание над highway — нельзя пересекаться с highway уровнем
                int maxHighwayLevel = Math.max(hx, hz);
                if (maxHighwayLevel >= 0) {
                    int maxCellarsAllowed = cityLevel - maxHighwayLevel - 1;
                    maxcellars = Math.min(maxcellars, maxCellarsAllowed);
                }
                
                // Генерируем cellars
                if (maxcellars > 0 && maxcellars >= mincellars) {
                    cellars = mincellars + rand.nextInt(maxcellars - mincellars + 1);
                } else {
                    cellars = 0;
                }
            }
            // Для мульти-зданий (не top-left) floors и cellars уже скопированы из top-left выше
            
            int n = floors + cellars + 1;
            
            // Connections: вычисляем с учётом позиции в multibuilding
            // КРИТИЧНО: как в оригинале - проверяем isCity соседа и doorway chance
            if (connectionAtX == null) {
                connectionAtX = new boolean[n];
                connectionAtZ = new boolean[n];
                float doorway = profile.getBuildingDoorwayChance();
                for (int i = 0; i < n; i++) {
                    ChunkPos west = new ChunkPos(pos.x - 1, pos.z);
                    ChunkPos north = new ChunkPos(pos.x, pos.z - 1);
                    
                    // Для multibuilding: блокируем connections на внутренних границах
                    boolean canConnectX = true;
                    boolean canConnectZ = true;
                    
                    if (multiPos.isMulti()) {
                        // Если справа от нас (x+1) есть другая часть того же multibuilding - нет connection
                        if (multiPos.x() < multiPos.w() - 1) {
                            canConnectX = false; // Внутренняя граница multibuilding
                        }
                        // Если внизу от нас (z+1) есть другая часть того же multibuilding - нет connection
                        if (multiPos.z() < multiPos.h() - 1) {
                            canConnectZ = false; // Внутренняя граница multibuilding
                        }
                    }
                    
                    // КРИТИЧНО: как в оригинале - проверяем isCity соседа и doorway chance
                    // Оригинал: connectionAtX[i] = isCity(coord.west(), provider) && (rand.nextFloat() < profile.BUILDING_DOORWAYCHANCE);
                    connectionAtX[i] = canConnectX && City.isCity(west, config, world) && rand.nextFloat() < doorway;
                    connectionAtZ[i] = canConnectZ && City.isCity(north, config, world) && rand.nextFloat() < doorway;
                }
                
                if (multiPos.isMulti()) {
                    ModLogger.debug("Multibuilding part ({}, {}) connections: canConnectX={}, canConnectZ={}", 
                        pos.x, pos.z, multiPos.x() >= multiPos.w() - 1, multiPos.z() >= multiPos.h() - 1);
                }
            }

            float corridorChance = profile.getCorridorChance();
            boolean xrc;
            boolean zrc;
            if (hasBuilding && cellars > 0) {
                xrc = false;
                zrc = false;
            } else {
                xrc = rand.nextFloat() < corridorChance;
                zrc = rand.nextFloat() < corridorChance;
            }

            BuildingInfo info = new BuildingInfo(pos, config, true, true, actualBuildingType, floors, cellars,
                    ground, cityLevel, null, connectionAtX, connectionAtZ, frontType, multiPos, multiBuilding, null, 0f, hx, hz,
                    false, false, null, xrc, zrc, selectedBricksPalette, selectedGlassPalette, selectedGlassSidePalette, doorBlock);
            CACHE.put(pos, info);
            return info;
        } finally {
            // Удаляем чанк из IN_PROGRESS после завершения вычисления
            inProgress.remove(pos);
        }
    }
    
    /**
     * Проверяет, является ли building частью multibuilding по имени.
     * Части multibuilding имеют формат: название + 2 цифры (center00, library01, town10, и т.д.)
     * Одиночные здания: building1, building2, cabin, radiotower
     */
    private static boolean isMultiBuildingPart(String buildingId) {
        if (buildingId == null || buildingId.length() < 2) {
            return false;
        }
        
        // Убираем namespace если есть (lostcities:center00 -> center00)
        String name = buildingId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }
        
        // Проверяем последние 2 символа - должны быть цифры
        if (name.length() >= 2) {
            char last1 = name.charAt(name.length() - 1);
            char last2 = name.charAt(name.length() - 2);
            
            // Если оба последних символа - цифры, это часть multibuilding
            if (Character.isDigit(last1) && Character.isDigit(last2)) {
                return true;
            }
        }
        
        return false;
    }

    public int getBuildingHeight() {
        if (!hasBuilding) return 0;
        return (floors + cellars + 1) * FLOOR_HEIGHT;
    }

    /** Y базового уровня этажа. Оригинал: первый этаж = getCityGroundLevel(), этажи выше +6. */
    public int getYForFloor(int floorIndex) {
        return getCityGroundLevel() + floorIndex * FLOOR_HEIGHT;
    }

    private com.lostcity.assets.CompiledPalette cachedCompiledPalette = null;

    public com.lostcity.assets.CompiledPalette getCompiledPalette(net.minecraft.world.StructureWorldAccess world) {
        if (cachedCompiledPalette != null) {
            return cachedCompiledPalette;
        }

        com.lostcity.assets.Palette stylePalette = null;
        if (isCity) {
            com.lostcity.assets.CityStyle cityStyle = City.getCityStyle(chunkPos, config, world);
            if (cityStyle != null && cityStyle.getStyle() != null) {
                com.lostcity.assets.Style style = com.lostcity.assets.AssetRegistries.getStyle(cityStyle.getStyle());
                if (style != null) {
                    long seed = world != null ? world.getSeed() : 0;
                    QualityRandom styleRand = new QualityRandom(
                        seed + (long) chunkPos.z * 341873128712L + (long) chunkPos.x * 132897987541L);
                    stylePalette = style.getRandomPalette(world, styleRand);
                }
            }
        } else {
            com.lostcity.assets.Style outsideStyle = com.lostcity.assets.AssetRegistries.getStyle("lostcities:outside");
            if (outsideStyle != null) {
                long seed = world != null ? world.getSeed() : 0;
                QualityRandom styleRand = new QualityRandom(
                    seed + (long) chunkPos.z * 341873128712L + (long) chunkPos.x * 132897987541L);
                stylePalette = outsideStyle.getRandomPalette(world, styleRand);
            }
        }

        if (stylePalette == null) {
            com.lostcity.assets.Palette commonPalette = com.lostcity.assets.AssetRegistries.getPalette("lostcities:common");
            com.lostcity.assets.Palette defaultPalette = com.lostcity.assets.AssetRegistries.getPalette("lostcities:default");
            if (commonPalette != null) {
                stylePalette = commonPalette;
                if (defaultPalette != null) {
                    stylePalette.merge(defaultPalette);
                }
            } else if (defaultPalette != null) {
                stylePalette = defaultPalette;
            } else {
                stylePalette = new com.lostcity.assets.Palette("empty"); // Empty
            }
        }

        cachedCompiledPalette = new com.lostcity.assets.CompiledPalette(stylePalette);

        if (hasBuilding && buildingType != null) {
            com.lostcity.assets.Building building = com.lostcity.assets.AssetRegistries.getBuilding(buildingType);
            if (building != null && building.getRefPaletteName() != null) {
                com.lostcity.assets.Palette buildingPalette = com.lostcity.assets.AssetRegistries.getPalette(building.getRefPaletteName());
                if (buildingPalette != null) {
                    cachedCompiledPalette = new com.lostcity.assets.CompiledPalette(cachedCompiledPalette, buildingPalette);
                }
            }
        }

        return cachedCompiledPalette;
    }

    public boolean hasXCorridor() {
        if (!xRailCorridor) {
            return false;
        }
        BuildingInfo i = getXmin();
        while (i != null && i.canRailGoThrough() && i.xRailCorridor) {
            i = i.getXmin();
        }
        if (i == null || (!i.hasBuilding) || i.cellars == 0) {
            return false;
        }
        i = getXmax();
        while (i != null && i.canRailGoThrough() && i.xRailCorridor) {
            i = i.getXmax();
        }
        return i != null && !((!i.hasBuilding) || i.cellars == 0);
    }

    public boolean hasZCorridor() {
        if (!zRailCorridor) {
            return false;
        }
        BuildingInfo i = getZmin();
        while (i != null && i.canRailGoThrough() && i.zRailCorridor) {
            i = i.getZmin();
        }
        if (i == null || (!i.hasBuilding) || i.cellars == 0) {
            return false;
        }
        i = getZmax();
        while (i != null && i.canRailGoThrough() && i.zRailCorridor) {
            i = i.getZmax();
        }
        return i != null && !((!i.hasBuilding) || i.cellars == 0);
    }

    public boolean canRailGoThrough() {
        if (!isCity) {
            return false;
        }
        if (!hasBuilding) {
            return true;
        }
        return cellars == 0;
    }

    /** Y первого этажа города (улицы и здания). */
    public int getCityGroundLevel() {
        return groundLevel + cityLevel * FLOOR_HEIGHT;
    }

    public BuildingInfo getXmin() { return get(new ChunkPos(chunkPos.x - 1, chunkPos.z)); }
    public BuildingInfo getXmax() { return get(new ChunkPos(chunkPos.x + 1, chunkPos.z)); }
    public BuildingInfo getZmin() { return get(new ChunkPos(chunkPos.x, chunkPos.z - 1)); }
    public BuildingInfo getZmax() { return get(new ChunkPos(chunkPos.x, chunkPos.z + 1)); }

    /**
     * Получить BuildingInfo соседа. Критично передавать world (getCurrentWorld), иначе
     * isCity/cityLevel неверны → мосты «обрываются», не сходятся с городами.
     * Оригинал: getBuildingInfo(key, provider) всегда с provider.getWorld().
     */
    private BuildingInfo get(ChunkPos p) {
        StructureWorldAccess w = ChunkHeightmap.getCurrentWorld();
        return BuildingInfo.get(p, config, w);
    }

    /** Улица/парк расширяет дорогу (можно соединять с соседями). Оригинал: не extend если elevated park. */
    public boolean doesRoadExtendTo() {
        if (!isCity || hasBuilding) return false;
        return !isElevatedParkSection();
    }

    /** Парк, окружённый многими парками (>= threshold), не продлевает дорогу. Оригинал: isElevatedParkSection. */
    public boolean isElevatedParkSection() {
        if (!isStreetOrParkSection() || streetType != StreetType.PARK) return false;
        int threshold = config.getActiveProfile().getParkStreetThreshold();
        int n = 0;
        if (getXmin().isStreetOrParkSection()) n++;
        if (getXmax().isStreetOrParkSection()) n++;
        if (getZmin().isStreetOrParkSection()) n++;
        if (getZmax().isStreetOrParkSection()) n++;
        if (getXmin().getZmin().isStreetOrParkSection()) n++;
        if (getXmin().getZmax().isStreetOrParkSection()) n++;
        if (getXmax().getZmin().isStreetOrParkSection()) n++;
        if (getXmax().getZmax().isStreetOrParkSection()) n++;
        return n >= threshold;
    }

    public static boolean hasRoadConnection(BuildingInfo i1, BuildingInfo i2) {
        if (!i1.doesRoadExtendTo() || !i2.doesRoadExtendTo()) return false;
        return Math.abs(i1.cityLevel - i2.cityLevel) <= 1;
    }

    public boolean isStreetOrParkSection() {
        return isCity && !hasBuilding;
    }

    /** Есть ли связь по X (запад) на уровне этажа. Для проёмов/дверей. Оригинал: hasConnectionAtX. */
    public boolean hasConnectionAtX(int level) {
        if (!isCity || connectionAtX == null) return false;
        if (multiBuildingPos.isRightSide()) return false;
        if (level < 0 || level >= connectionAtX.length) return false;
        
        // КРИТИЧНО: проверка META_DONTCONNECT для этажа (как в оригинале)
        if (isValidFloor(level - cellars)) {
            BuildingPart floor = getFloor(level - cellars);
            if (floor != null && floor.getMetaBoolean("dontconnect")) {
                return false; // No connection supported
            }
        }
        
        // КРИТИЧНО: если есть front part от соседа, связь всегда есть (как в оригинале)
        if (getXmin().hasFrontPartFrom(this)) {
            return true;
        }
        
        return connectionAtX[level];
    }

    /** Есть ли связь по Z (север) на уровне этажа. Оригинал: hasConnectionAtZ. */
    public boolean hasConnectionAtZ(int level) {
        if (!isCity || connectionAtZ == null) return false;
        if (multiBuildingPos.isBottomSide()) return false;
        if (level < 0 || level >= connectionAtZ.length) return false;
        
        // КРИТИЧНО: проверка META_DONTCONNECT для этажа (как в оригинале)
        if (isValidFloor(level - cellars)) {
            BuildingPart floor = getFloor(level - cellars);
            if (floor != null && floor.getMetaBoolean("dontconnect")) {
                return false; // No connection supported
            }
        }
        
        // КРИТИЧНО: если есть front part от соседа, связь всегда есть (как в оригинале)
        if (getZmin().hasFrontPartFrom(this)) {
            return true;
        }
        
        return connectionAtZ[level];
    }

    /** Улица: нужно ли ставить фасад (front) со стороны соседа adj. Оригинал: hasFrontPartFrom. */
    public boolean hasFrontPartFrom(BuildingInfo adj) {
        StreetType st = streetType;
        boolean elevated = isElevatedParkSection();
        if (elevated) {
            st = StreetType.PARK;
        }
        
        // КРИТИЧНО: проверка как в оригинале - только для NORMAL улиц, с проверкой высоты
        if (adj.hasBuilding && adj.frontType != null && st == StreetType.NORMAL && cityLevel < adj.cityLevel + adj.getNumFloors()) {
            // Проверка на highway - если есть highway, front part не ставится
            if (getMaxHighwayLevel() >= 0) {
                return false;
            }
            
            // Проверка на railway (упрощенная версия - Railway класс отсутствует)
            // TODO: В будущем добавить полную проверку Railway
            
            // Проверка META_DONTCONNECT для этажа здания на уровне улицы
            int local = adj.globalToLocal(cityLevel);
            if (adj.isValidFloor(local)) {
                BuildingPart floor = adj.getFloor(local);
                if (floor != null && floor.getMetaBoolean("dontconnect")) {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }

    /** Есть ли связь с улицей по X на уровне этажа (оригинал: hasConnectionAtXFromStreet). */
    public boolean hasConnectionAtXFromStreet(int level) {
        if (!isCity || connectionAtX == null) return false;
        // Проверка для multibuilding: если мы на правой стороне, связи нет
        if (multiBuildingPos != null && multiBuildingPos.isRightSide()) {
            return false;
        }
        if (level < 0 || level >= connectionAtX.length) return false;
        // Если есть front part от соседа, всегда есть связь
        if (hasFrontPartFrom(getXmin())) {
            return true;
        }
        // Связь с улицей если connectionAtX[level] == true
        return connectionAtX[level];
    }

    /** Есть ли связь с улицей по Z на уровне этажа (оригинал: hasConnectionAtZFromStreet). */
    public boolean hasConnectionAtZFromStreet(int level) {
        if (!isCity || connectionAtZ == null) return false;
        // Проверка для multibuilding: если мы на нижней стороне, связи нет
        if (multiBuildingPos != null && multiBuildingPos.isBottomSide()) {
            return false;
        }
        if (level < 0 || level >= connectionAtZ.length) return false;
        // Если есть front part от соседа, всегда есть связь
        if (getZmin().hasFrontPartFrom(this)) {
            return true;
        }
        // Связь с улицей если connectionAtZ[level] == true
        return connectionAtZ[level];
    }

    // === Преобразование уровней (оригинал: localToGlobal/globalToLocal) ===

    /**
     * Преобразует локальный уровень этажа в глобальный (учитывая cityLevel).
     * Оригинал: localToGlobal(int l) { return l + cityLevel; }
     */
    public int localToGlobal(int localLevel) {
        return localLevel + cityLevel;
    }

    /**
     * Преобразует глобальный уровень в локальный для конкретного здания.
     * Оригинал: globalToLocal(int l) { return l - cityLevel; }
     */
    public int globalToLocal(int globalLevel) {
        return globalLevel - cityLevel;
    }

    /**
     * Проверяет, является ли локальный уровень этажа валидным.
     * Оригинал: isValidFloor(int l) { return (l + cellars) >= 0 && (l + cellars) < floorTypes.length; }
     */
    public boolean isValidFloor(int localLevel) {
        int level = localLevel + cellars;
        return level >= 0 && level < (floors + cellars + 1);
    }

    /**
     * Получить часть этажа по локальному индексу.
     * Оригинал: getFloor(int l) { return floorTypes[l + cellars]; }
     * У нас получаем часть из building по floorIndex.
     */
    public BuildingPart getFloor(int localLevel) {
        if (!hasBuilding || buildingType == null) return null;
        Building building = AssetRegistries.getBuilding(buildingType);
        if (building == null) return null;
        
        // Преобразуем localLevel в floorIndex (localLevel может быть отрицательным для cellars)
        int floorIndex = localLevel;
        boolean isTopFloor = (floorIndex == floors);
        
        // Ищем часть для этого этажа
        for (Building.PartRef partRef : building.getParts()) {
            if (partRef.top == isTopFloor) {
                return AssetRegistries.getPart(partRef.part);
            }
        }
        
        // Если не нашли, используем первую доступную часть
        if (!building.getParts().isEmpty()) {
            Building.PartRef firstPart = building.getParts().get(0);
            return AssetRegistries.getPart(firstPart.part);
        }
        
        return null;
    }

    /**
     * Получить количество этажей (оригинал: getNumFloors()).
     */
    public int getNumFloors() {
        return floors;
    }

    /**
     * Проверка связи на уровне с ориентацией (оригинал: hasConnectionAt(int level, Orientation orientation)).
     */
    public boolean hasConnectionAt(int level, Orientation orientation) {
        return switch (orientation) {
            case X -> hasConnectionAtX(level);
            case Z -> hasConnectionAtZ(level);
        };
    }

    /** Магистраль на level — тоннель? Оригинал: isTunnel(level). */
    public boolean isTunnel(int level) {
        if (isCity) return cityLevel > level;
        var w = ChunkHeightmap.getCurrentWorld();
        if (w == null) return false;
        int h = ChunkHeightmap.getHeight(w, chunkPos.x, chunkPos.z);
        int highwayHeight = groundLevel + level * FLOOR_HEIGHT + 3;
        return h > highwayHeight;
    }

    /** Уровень воды для магистралей (dowater). */
    public int getWaterLevel() {
        int sl = config.getActiveProfile().getSeaLevel();
        return sl >= 0 ? sl : 63;
    }

    /** Ориентация для связи (X или Z). */
    public enum Orientation {
        X, Z
    }

    private StairDirection stairDir;
    private StairDirection actualStairDir;

    /** Направление уличной лестницы: мы на 1 уровень ниже соседа → лестница в сторону соседа. Только для street, не PARK. */
    public StairDirection getStairDirection() {
        if (stairDir != null) return stairDir;
        stairDir = computeStairDirection();
        return stairDir;
    }

    private StairDirection computeStairDirection() {
        if (streetType == StreetType.PARK || hasBuilding || !isCity) return null;
        if (cityLevel == getXmin().cityLevel - 1 && !getXmin().hasBuilding && getXmin().isCity) return StairDirection.XMIN;
        if (cityLevel == getXmax().cityLevel - 1 && !getXmax().hasBuilding && getXmax().isCity) return StairDirection.XMAX;
        if (cityLevel == getZmin().cityLevel - 1 && !getZmin().hasBuilding && getZmin().isCity) return StairDirection.ZMIN;
        if (cityLevel == getZmax().cityLevel - 1 && !getZmax().hasBuilding && getZmax().isCity) return StairDirection.ZMAX;
        return null;
    }

    /** Учитывает приоритет: при конкуренции лестница только у чанка с большим stairPriority. */
    public StairDirection getActualStairDirection() {
        if (actualStairDir != null) return actualStairDir;
        StairDirection d = getStairDirection();
        if (d != null) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BuildingInfo adj = BuildingInfo.get(new ChunkPos(chunkPos.x + dx, chunkPos.z + dz), config);
                    if (adj.getStairDirection() != null && adj.stairPriority > stairPriority) {
                        d = null;
                        break;
                    }
                }
                if (d == null) break;
            }
        }
        actualStairDir = d;
        return actualStairDir;
    }

    // === МОСТИКИ (Bridges) ===

    /**
     * Проверяет, есть ли X-мост (вдоль оси X) в этом чанке. Оригинал: hasXBridge.
     * Мост проходит через несколько негородских чанков и соединяет два города.
     * Приоритет чанкам с чётным Z-координатом (чтобы избежать параллельных мостов).
     */
    public BuildingPart hasXBridge() {
        if (xBridgeTypeCalculated) return xBridgeType;
        xBridgeTypeCalculated = true;
        xBridgeType = null;

        // Оригинал: проверяем флаг xBridge и isSuitableForBridge
        if (!xBridge) return null;
        if (!isSuitableForBridge(this)) return null;

        // Приоритет чанкам с чётным chunkZ — если нечётный, проверяем соседей по Z
        if (chunkPos.z % 2 != 0) {
            BuildingInfo zmin = getZmin(), zmax = getZmax();
            if (zmin.hasXBridge() != null || zmax.hasXBridge() != null) return null;
        }

        // Ищем конечные точки моста: от xmin до xmax (пока !isCity && xBridge && suitable)
        BuildingPart bt = bridgeType;
        BuildingInfo i = getXmin();
        while (!i.isCity && i.xBridge && isSuitableForBridge(i)) {
            if (chunkPos.z % 2 != 0 && (i.getZmin().hasXBridge() != null || i.getZmax().hasXBridge() != null)) return null;
            bt = i.bridgeType;
            i = i.getXmin();
        }
        if (!i.isCity || i.hasBuilding || i.cityLevel > 0) return null;

        BuildingInfo minimum = i;

        i = getXmax();
        while (!i.isCity && i.xBridge && isSuitableForBridge(i)) {
            if (chunkPos.z % 2 != 0 && (i.getZmin().hasXBridge() != null || i.getZmax().hasXBridge() != null)) return null;
            i = i.getXmax();
        }
        if (!i.isCity || i.hasBuilding || i.cityLevel > 0) return null;

        // Кэшируем для всех чанков моста (от minimum до текущего)
        xBridgeType = bt;
        i = i.getXmin();
        ChunkPos minPos = minimum.chunkPos;
        while (!i.chunkPos.equals(minPos)) {
            i.xBridgeType = bt;
            i.xBridgeTypeCalculated = true;
            i.zBridgeType = null;
            i.zBridgeTypeCalculated = true;
            i = i.getXmin();
        }
        return xBridgeType;
    }

    /**
     * Проверяет, есть ли Z-мост (вдоль оси Z). Оригинал: hasZBridge.
     * Приоритет чанкам с чётным X-координатом.
     */
    public BuildingPart hasZBridge() {
        if (zBridgeTypeCalculated) return zBridgeType;
        zBridgeTypeCalculated = true;
        zBridgeType = null;

        if (!zBridge) return null;
        if (!isSuitableForBridge(this)) return null;
        if (hasXBridge() != null) return null; // X-мост имеет приоритет

        // Приоритет чанкам с чётным chunkX
        if (chunkPos.x % 2 != 0) {
            BuildingInfo xmin = getXmin(), xmax = getXmax();
            if (xmin.hasZBridge() != null || xmax.hasZBridge() != null) return null;
        }

        BuildingPart bt = bridgeType;
        BuildingInfo i = getZmin();
        while (!i.isCity && i.zBridge && isSuitableForBridge(i)) {
            if (i.hasXBridge() != null) return null;
            if (chunkPos.x % 2 != 0 && (i.getXmin().hasZBridge() != null || i.getXmax().hasZBridge() != null)) return null;
            bt = i.bridgeType;
            i = i.getZmin();
        }
        if (!i.isCity || i.hasBuilding || i.cityLevel > 0) return null;

        BuildingInfo minimum = i;

        i = getZmax();
        while (!i.isCity && i.zBridge && isSuitableForBridge(i)) {
            if (i.hasXBridge() != null) return null;
            if (chunkPos.x % 2 != 0 && (i.getXmin().hasZBridge() != null || i.getXmax().hasZBridge() != null)) return null;
            i = i.getZmax();
        }
        if (!i.isCity || i.hasBuilding || i.cityLevel > 0) return null;

        // Кэшируем
        zBridgeType = bt;
        i = i.getZmin();
        ChunkPos minPos = minimum.chunkPos;
        while (!i.chunkPos.equals(minPos)) {
            i.zBridgeType = bt;
            i.zBridgeTypeCalculated = true;
            i.xBridgeType = null;
            i.xBridgeTypeCalculated = true;
            i = i.getZmin();
        }
        return zBridgeType;
    }

    /**
     * Проверяет, подходит ли чанк для моста. Оригинал: isSuitableForBridge.
     * Мост может идти через чанк если: cityLevel меньше чем у исходного ИЛИ водный биом.
     * При проверке «этого» чанка (info==this): не-городской чанк всегда подходит (долина).
     */
    private boolean isSuitableForBridge(BuildingInfo info) {
        // TODO: monorail для Space профиля
        if (info.cityLevel < this.cityLevel) return true;
        if (isWaterBiome(info)) return true;
        if (info == this && !this.isCity) return true;
        return false;
    }

    private static boolean isWaterBiome(BuildingInfo info) {
        StructureWorldAccess w = ChunkHeightmap.getCurrentWorld();
        if (w == null) return false;
        // Биом в центре чанка. Берём из шума генератора, а не через w.getBiome():
        // последний ходит в ChunkRegion.getChunk() и для соседних чанков за границей
        // региона на 1.20.6+ роняет генерацию с "Requested chunk unavailable".
        int cx = (info.chunkPos.x << 4) + 8;
        int cz = (info.chunkPos.z << 4) + 8;
        var biome = TerrainHeight.sampleBiome(w, cx, 64, cz);
        if (biome == null) return false;
        return biome.isIn(net.minecraft.registry.tag.BiomeTags.IS_OCEAN)
            || biome.isIn(net.minecraft.registry.tag.BiomeTags.IS_DEEP_OCEAN)
            || biome.isIn(net.minecraft.registry.tag.BiomeTags.IS_RIVER)
            || biome.isIn(net.minecraft.registry.tag.BiomeTags.IS_BEACH);
    }
    
    // === MULTIBUILDING CITY LEVEL (Этап 3.2) ===
    
    /**
     * Вычислить усредненный cityLevel для всех частей multibuilding. Оригинал: getAverageCityLevel().
     * 
     * @param multiPos Позиция в multibuilding
     * @param pos Позиция текущего чанка
     * @param profile Профиль
     * @param world Мир (может быть null)
     * @return Усредненный cityLevel
     */
    private static int getAverageCityLevel(MultiPos multiPos, ChunkPos pos, ProfileConfig profile, StructureWorldAccess world) {
        int level = 0;
        int topX = pos.x - multiPos.x();
        int topZ = pos.z - multiPos.z();
        int count = 0;
        
        for (int x = 0; x < multiPos.w(); x++) {
            for (int z = 0; z < multiPos.h(); z++) {
                ChunkPos key = new ChunkPos(topX + x, topZ + z);
                level += ChunkHeightmap.getCityLevel(key, profile, world);
                count++;
            }
        }
        
        return count > 0 ? level / count : 0;
    }
    
    // === BUILDING POSSIBILITY (Этап 2.3) ===
    
    /**
     * Проверить возможность генерации здания в чанке. Оригинал: checkBuildingPossibility().
     * 
     * @param pos Позиция чанка
     * @param config Конфигурация мода
     * @param profile Профиль
     * @param multiPos Позиция в multibuilding
     * @param cityLevel Уровень города
     * @param hx Уровень X-магистрали
     * @param hz Уровень Z-магистрали
     * @param rand Генератор случайных чисел
     * @return true если здание может быть сгенерировано
     */
    private static boolean checkBuildingPossibility(ChunkPos pos, LostCityConfig config, ProfileConfig profile,
                                                     MultiPos multiPos, int cityLevel, int hx, int hz, Random rand) {
        // Если часть multibuilding - всегда true
        if (multiPos.isMulti()) {
            return true;
        }
        
        // Проверка по buildingChance
        float buildingChance = profile.getBuildingChance();
        // Этап 3.1: CityStyle buildingChance (полная реализация)
        // В оригинале используется CityStyle.getBuildingChance() который может переопределить profile.getBuildingChance()
        com.lostcity.assets.CityStyle cityStyle = City.getCityStyle(pos, config, null); // world может быть null
        if (cityStyle != null && cityStyle.getBuildingChance() != null) {
            buildingChance = cityStyle.getBuildingChance();
        }
        
        float bc = rand.nextFloat();
        if (bc >= buildingChance) {
            // Случайность говорит, что здания не должно быть
            return false;
        }
        
        // Проверка highway levels
        int maxHighwayLevel = Math.max(hx, hz);
        if (maxHighwayLevel >= 0) {
            // Мы над магистралью. Проверяем, есть ли место для здания
            // Здание разрешено, если оно выше максимальной магистрали + 1
            return cityLevel > maxHighwayLevel + 1;
        }
        
        // Этап 2.3: Проверка railway levels (отложено - Railway класс отсутствует)
        // В оригинале проверяется Railway.getRailChunkType() и если есть подземная станция, здание не генерируется
        // Если есть railway на поверхности, здание разрешено только если cityLevel > railwayLevel + 1
        // TODO: В будущем можно добавить полную поддержку Railway системы
        // if (hasRailway(coord, provider, profile)) {
        //     Railway.RailChunkInfo info = Railway.getRailChunkType(coord, provider, profile);
        //     if (info.getType() == RailChunkType.STATION_UNDERGROUND) {
        //         return false;  // Нет здания прямо над подземной станцией
        //     } else {
        //         int maxh = info.getLevel();
        //         return cityLevel > maxh + 1;  // Здание разрешено, если выше максимального уровня железной дороги + 1
        //     }
        // }
        
        // Общий случай
        return true;
    }
    
    // === TERRAIN CORRECTION (Этап 2.1) ===
    
    /**
     * Класс для хранения минимальной и максимальной высоты. Оригинал: MinMax.
     */
    public static class MinMax {
        public int min;
        public int max;

        public MinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public MinMax(MinMax mm) {
            min = mm.min;
            max = mm.max;
        }

        public MinMax() {
            min = max = 100000;
        }
    }
    
    /**
     * Получить минимальную высоту города в углу четырёх чанков (если это городской чанк).
     * info: ссылка на нижний-правый чанк. Позиция 0,0 этого чанка является референсом.
     * Возвращает 100000 если угол не прилегает к городскому чанку.
     * Также возвращает 100000 если все углы городские или ландшафтные (такой угол
     * не должен влиять на ландшафт за пределами этих чанков).
     * Это версия уровня 0, которая смотрит только на текущий угол чанка.
     * Оригинал: getLowestCityHeightAtChunkCorner()
     */
    public int getLowestCityHeightAtChunkCorner() {
        BuildingInfo info00 = getXmin().getZmin();
        BuildingInfo info01 = getXmin();
        BuildingInfo info10 = getZmin();
        if (isCity && info10.isCity && info00.isCity && info01.isCity) {
            return 100000;
        }
        if (!isCity && !info10.isCity && !info00.isCity && !info01.isCity) {
            return 100000;
        }
        // Если мы здесь, у нас смесь городских и обычных чанков
        int h = getCityHeightForChunk();
        h = Math.min(h, info01.getCityHeightForChunk());
        h = Math.min(h, info10.getCityHeightForChunk());
        h = Math.min(h, info00.getCityHeightForChunk());
        return h;
    }

    /**
     * Используется для коррекции террейна и указывает желаемый уровень,
     * к которому должны интерполироваться прилегающие террейны.
     * Оригинал: getCityHeightForChunk()
     */
    public int getCityHeightForChunk() {
        ProfileConfig profile = config.getActiveProfile();
        if (isCity) {
            return getCityGroundLevel();
        } else {
            if (isOcean()) {
                return groundLevel - profile.getOceanCorrectionBorder();
            } else {
                return 100000;
            }
        }
    }
    
    /**
     * Проверка, является ли чанк океаном. Оригинал: isOcean()
     */
    private boolean isOcean() {
        return isWaterBiome(this);
    }
    
    /**
     * Учитывая прилегающие (городские) чанки, вычислить желаемую высоту для интерполяции
     * ландшафта (минимум/максимум). Вычисляется для референсной позиции этого чанка (точка 0,0).
     * Это версия уровня 1, которая смотрит только на прилегающие высоты.
     * Оригинал: getDesiredMaxHeightL1()
     */
    private MinMax getDesiredMaxHeightL1() {
        if (desiredMaxHeight1 == null) {
            int h = getLowestCityHeightAtChunkCorner();
            ProfileConfig profile = config.getActiveProfile();
            int cx = chunkPos.x;
            int cz = chunkPos.z;

            // @todo build limit
            if (h < 256) {
                // Высота L0 в этом углу фиксирована, возвращаем её
                desiredMaxHeight1 = new MinMax(
                        h + LostCityFeature.getRandomizedOffset(cx, cz, profile.getTerrainFixLowerMinOffset(), profile.getTerrainFixLowerMaxOffset()),
                        h + LostCityFeature.getRandomizedOffset(cx, cz, profile.getTerrainFixUpperMinOffset(), profile.getTerrainFixUpperMaxOffset()));
                return desiredMaxHeight1;
            }

            MinMax minMax = new MinMax();

            getXmin().getZmin().updateMinMaxL1(minMax, 25 + LostCityFeature.getHeightOffsetL1(cx - 1, cz - 1));
            getXmin().updateMinMaxL1(minMax, 20 + LostCityFeature.getHeightOffsetL1(cx - 1, cz));
            getXmin().getZmax().updateMinMaxL1(minMax, 25 + LostCityFeature.getHeightOffsetL1(cx - 1, cz + 1));

            getZmin().updateMinMaxL1(minMax, 20 + LostCityFeature.getHeightOffsetL1(cx, cz - 1));
            getZmax().updateMinMaxL1(minMax, 20 + LostCityFeature.getHeightOffsetL1(cx, cz + 1));

            getXmax().getZmin().updateMinMaxL1(minMax, 25 + LostCityFeature.getHeightOffsetL1(cx + 1, cz - 1));
            getXmax().updateMinMaxL1(minMax, 20 + LostCityFeature.getHeightOffsetL1(cx + 1, cz));
            getXmax().getZmax().updateMinMaxL1(minMax, 25 + LostCityFeature.getHeightOffsetL1(cx + 1, cz + 1));

            desiredMaxHeight1 = minMax;
        }
        return desiredMaxHeight1;
    }

    /**
     * Учитывая прилегающие (городские) чанки, вычислить желаемую высоту для интерполяции
     * ландшафта. Вычисляется для референсной позиции этого чанка (точка 0,0).
     * Это версия уровня 2, которая смотрит на высоты L1 прилегающих чанков.
     * Оригинал: getDesiredMaxHeightL2()
     */
    public MinMax getDesiredMaxHeightL2() {
        if (desiredTerrainCorrectionHeights == null) {
            MinMax mm = getDesiredMaxHeightL1();
            // @todo build limit
            if (mm.min < 256) {
                // Высота L1 в этом углу фиксирована, возвращаем её
                desiredTerrainCorrectionHeights = new MinMax(mm);
                return desiredTerrainCorrectionHeights;
            }

            int cx = chunkPos.x;
            int cz = chunkPos.z;

            MinMax minMax = new MinMax();

            getXmin().getZmin().updateMinMaxL2(minMax, 25 + LostCityFeature.getHeightOffsetL2(cx - 1, cz - 1));
            getXmin().updateMinMaxL2(minMax, 20 + LostCityFeature.getHeightOffsetL2(cx - 1, cz));
            getXmin().getZmax().updateMinMaxL2(minMax, 25 + LostCityFeature.getHeightOffsetL2(cx - 1, cz + 1));

            getZmin().updateMinMaxL2(minMax, 20 + LostCityFeature.getHeightOffsetL2(cx, cz - 1));
            getZmax().updateMinMaxL2(minMax, 20 + LostCityFeature.getHeightOffsetL2(cx, cz + 1));

            getXmax().getZmin().updateMinMaxL2(minMax, 25 + LostCityFeature.getHeightOffsetL2(cx + 1, cz - 1));
            getXmax().updateMinMaxL2(minMax, 20 + LostCityFeature.getHeightOffsetL2(cx + 1, cz));
            getXmax().getZmax().updateMinMaxL2(minMax, 25 + LostCityFeature.getHeightOffsetL2(cx + 1, cz + 1));
            desiredTerrainCorrectionHeights = minMax;
        }
        return desiredTerrainCorrectionHeights;
    }

    public void updateMinMaxL2(MinMax minMax, int offs) {
        MinMax h = getDesiredMaxHeightL1();
        if ((h.min - offs) < minMax.min) {
            minMax.min = h.min - offs;
        }
        if ((h.max + offs) < minMax.max) {
            minMax.max = h.max + offs;
        }
    }

    private void updateMinMaxL1(MinMax minMax, int offs) {
        int h = getLowestCityHeightAtChunkCorner();
        if ((h - offs) < minMax.min) {
            minMax.min = h - offs;
        }
        if ((h + offs) < minMax.max) {
            minMax.max = h + offs;
        }
    }
}
