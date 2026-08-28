package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.assets.AssetRegistries;
import com.lostcity.assets.Building;
import com.lostcity.assets.BuildingPart;
import com.lostcity.assets.CompiledPalette;
import com.lostcity.assets.HighwayParts;
import com.lostcity.assets.Palette;
import com.lostcity.assets.StreetParts;
import com.lostcity.config.LostCityConfig;
import com.lostcity.config.ProfileConfig;
import com.lostcity.util.ModLogger;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.Blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * Основной Feature для генерации Lost City
 * Портирован из LostCityFeature (оригинальный Forge мод)
 * 
 * FABRIC VERSION с Yarn mappings
 * ШАГ 4: Добавлена базовая генерация террейна
 */
public class LostCityFeature extends Feature<DefaultFeatureConfig> {
    
    // Счётчик вызовов для статистики
    private static int callCount = 0;
    private static long lastLogTime = 0;
    
    // === TERRAIN CORRECTION (Этап 2.1) ===
    private static final java.util.Random RANDOMIZED_OFFSET = new java.util.Random();
    private static final java.util.Random RANDOMIZED_OFFSET_L1 = new java.util.Random();
    private static final java.util.Random RANDOMIZED_OFFSET_L2 = new java.util.Random();
    
    /**
     * Получить рандомизированное смещение для коррекции террейна.
     * Оригинал: LostCityTerrainFeature.getRandomizedOffset()
     */
    public static int getRandomizedOffset(int chunkX, int chunkZ, int min, int max) {
        RANDOMIZED_OFFSET.setSeed(chunkZ * 256203221L + chunkX * 899809363L);
        return RANDOMIZED_OFFSET.nextInt(max - min + 1) + min;
    }

    /**
     * Получить смещение высоты уровня 1 для коррекции террейна.
     * Оригинал: LostCityTerrainFeature.getHeightOffsetL1()
     */
    public static int getHeightOffsetL1(int chunkX, int chunkZ) {
        RANDOMIZED_OFFSET_L1.setSeed(chunkZ * 341873128712L + chunkX * 132897987541L);
        return RANDOMIZED_OFFSET_L1.nextInt(5);
    }

    /**
     * Получить смещение высоты уровня 2 для коррекции террейна.
     * Оригинал: LostCityTerrainFeature.getHeightOffsetL2()
     */
    public static int getHeightOffsetL2(int chunkX, int chunkZ) {
        RANDOMIZED_OFFSET_L2.setSeed(chunkZ * 132897987541L + chunkX * 341873128712L);
        return RANDOMIZED_OFFSET_L2.nextInt(5);
    }
    
    public LostCityFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
        ModLogger.info("LostCityFeature instance created");
    }
    
    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        // Проверяем, включена ли генерация Lost Cities
        LostCityConfig config = LostCityMod.getConfig();
        if (config == null || "disabled".equals(config.selectedProfile)) {
            // Lost Cities отключен - используем vanilla генерацию
            return false;
        }
        
        // Получаем необходимые объекты из контекста
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        
        // Получаем chunk
        Chunk chunk = world.getChunk(origin);
        
        // Получаем координаты чанка из origin
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        
        // Инкрементируем счётчик
        callCount++;
        
        // Логируем каждый 10-й чанк
        if (callCount % 10 == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLogTime > 1000) {
                ModLogger.info("LostCityFeature.generate() called {} times. Current chunk: ({}, {})", 
                    callCount, chunkX, chunkZ);
                lastLogTime = currentTime;
            }
        }
        
        // Debug логирование (только в debug mode)
        if (ModLogger.isDebugEnabled()) {
            ModLogger.debug("=== LostCityFeature.generate() ===");
            ModLogger.debug("Chunk: ({}, {})", chunkX, chunkZ);
            ModLogger.debug("Origin: {}", origin);
            ModLogger.debug("Dimension: {}", world.toServerWorld().getRegistryKey().getValue());
            
            if (config != null) {
                ProfileConfig profile = config.getActiveProfile();
                ModLogger.debug("Active profile: {}", profile.getProfileName());
                ModLogger.debug("City chance: {}", profile.getCityChance());
                ModLogger.debug("Ground level: {}", profile.getGroundLevel());
            } else {
                ModLogger.error("Config is NULL! This should not happen!");
            }
            ModLogger.debug("=======================================");
        }
        
        // === КОНФИГУРАЦИЯ ===
        
        if (config == null) {
            ModLogger.error("Config is null in generate(), skipping chunk ({}, {})", chunkX, chunkZ);
            return false;
        }
        
        ProfileConfig profile = config.getActiveProfile();

        // === ШАГ 5: ГОРОД / НОРМАЛЬНЫЙ ЧАНК ===
        // Оригинал: и городские, и негородские чанки обрабатываются. В негородских — только магистрали
        // и мостики, чтобы магистрали шли далеко и пробивали горы.
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        // Этап 1.2: Передаём world для проверки высоты и Perlin noise
        boolean isCity = City.isCity(chunkPos, config, world);

        if (callCount <= 10) {
            ModLogger.info("Chunk ({}, {}) - isCity: {}, cityFactor: {}",
                chunkX, chunkZ, isCity,
                String.format("%.3f", City.getCityFactor(chunkPos, config, world)));
        }

        try {
            ChunkHeightmap.setCurrentWorld(world);
            if (isCity) {
                doGenerateCityChunk(world, chunk, chunkX, chunkZ, config, profile);
            } else {
                doNormalChunk(world, chunk, chunkX, chunkZ, config, profile);
            }
        } finally {
            ChunkHeightmap.clearCurrentWorld();
        }
        return true;
    }

    /**
     * Обработка негородского чанка. Оригинал: doNormalChunk.
     * Генерируем только магистрали и мостики, чтобы они шли далеко и пробивали горы.
     */
    private void doNormalChunk(StructureWorldAccess world, Chunk chunk, int chunkX, int chunkZ,
            LostCityConfig config, ProfileConfig profile) {
        ChunkDriver driver = new ChunkDriver(world, chunk);
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        BuildingInfo info = BuildingInfo.get(chunkPos, config, world);

        // Этап 2.1: Интеграция terrain correction с генерацией террейна
        // Оригинал: correctTerrainShape() вызывается в doNormalChunk для default профиля
        if (profile.isDefault() || profile.isSpace()) {
            // Получаем heightmap для обновления
            int heightmapHeight = ChunkHeightmap.getHeight(world, chunkX, chunkZ);
            // Создаем временный объект для heightmap (в оригинале используется ChunkHeightmap)
            // Для упрощения используем прямое значение
            correctTerrainShape(driver, info, chunkX, chunkZ, heightmapHeight);
        }

        if (info.highwayXLevel >= 0 || info.highwayZLevel >= 0) {
            generateHighways(driver, info, chunkX, chunkZ);
        }
        
        // Мостики между чанками
        Bridges.generateBridges(driver, info);

        Stuff.generateStuff(this, driver, info);
        info.executePostTodos();
    }

    private void doGenerateCityChunk(StructureWorldAccess world, Chunk chunk, int chunkX, int chunkZ,
            LostCityConfig config, ProfileConfig profile) {
        ChunkDriver driver = new ChunkDriver(world, chunk);
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

        BuildingInfo info = BuildingInfo.get(chunkPos, config, world);
        
        // Этап 2.2: Проверка void chunks для floating профиля (дополнительная проверка в генерации)
        // Оригинал: проверка в LostCityTerrainFeature перед doCityChunk
        if (info.isCity && profile.getCityAvoidVoid() && profile.isFloating()) {
            boolean isVoid = isVoidAt(driver, 2, 2) || isVoidAt(driver, 2, 14) || 
                            isVoidAt(driver, 14, 2) || isVoidAt(driver, 14, 14) || 
                            isVoidAt(driver, 8, 8);
            if (isVoid) {
                ModLogger.info("Chunk ({}, {}) is void in floating profile, skipping city generation", chunkX, chunkZ);
                // Переключаемся на нормальную генерацию (без города)
                doNormalChunk(world, chunk, chunkX, chunkZ, config, profile);
                return;
            }
        }
        
        int cityGroundLevel = info.getCityGroundLevel();

        int seaLevel = profile.getSeaLevel() >= 0 ? profile.getSeaLevel() : 63;
        // ВАЖНО: не пропускаем “низкие” city-чанки, иначе появляются жёсткие перепады (ванильный чанк рядом с city).
        // Оригинал решает воду/океан по-другому, но пропускать генерацию нельзя.

        if (ModLogger.isDebugEnabled()) {
            ModLogger.debug("=== CITY CHUNK ({}, {}) === cityLevel={}, cityGroundLevel={}",
                chunkX, chunkZ, info.cityLevel, cityGroundLevel);
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                generateColumn(driver, info, x, z, cityGroundLevel);
            }
        }

        if (callCount <= 5) {
            driver.logStats();
        }
        if (info.hasBuilding) {
            generateBuildings(driver, info, chunkX, chunkZ);
            generateVines(driver, info, chunkX, chunkZ);
        } else if (info.isStreetOrParkSection()) {
            fillToBedrockStreetBlock(driver, info);
            generateStreet(driver, info, chunkX, chunkZ);
            generateBorders(driver, info);
            generateStreetDecorations(driver, info, chunkX, chunkZ);
            generateLeavesForStreet(driver, info, chunkX, chunkZ);
            if (callCount <= 10) {
                ModLogger.info("City chunk ({}, {}) - street/park generated", chunkX, chunkZ);
            }
        }
        if (info.highwayXLevel >= 0 || info.highwayZLevel >= 0) {
            generateHighways(driver, info, chunkX, chunkZ);
        }

        Stuff.generateStuff(this, driver, info);
        info.executePostTodos();
    }

    private static final int FLOOR_HEIGHT = 6;
    private static final String META_SUPPORT = "support";

    /** Полная генерация магистралей. Оригинал: Highways.generateHighways. */
    private void generateHighways(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ) {
        int levelX = info.highwayXLevel;
        int levelZ = info.highwayZLevel;
        if (levelX == levelZ && levelX >= 0) {
            generateHighwayPart(driver, info, levelX, Transform.ROTATE_NONE, info.getXmax(), info.getZmax(), true);
        } else if (levelX >= 0 && levelZ >= 0) {
            if (levelX == 0) {
                generateHighwayPart(driver, info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), false);
                generateHighwayPart(driver, info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), false);
            } else {
                generateHighwayPart(driver, info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), false);
                generateHighwayPart(driver, info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), false);
            }
        } else {
            if (levelX >= 0) generateHighwayPart(driver, info, levelX, Transform.ROTATE_NONE, info.getZmin(), info.getZmax(), false);
            else if (levelZ >= 0) generateHighwayPart(driver, info, levelZ, Transform.ROTATE_90, info.getXmax(), info.getXmax(), false);
        }
    }

    private void generateHighwayPart(ChunkDriver driver, BuildingInfo info, int level, Transform transform,
            BuildingInfo adj1, BuildingInfo adj2, boolean bidirectional) {
        int highwayGroundLevel = info.groundLevel + level * FLOOR_HEIGHT;
        HighwayParts hp = HighwayParts.DEFAULT;
        CompiledPalette palette = AssetRegistries.getStreetPalette();
        if (palette == null) return;

        BuildingPart part = null;
        boolean shouldGenerateSupports = false;
        
        if (info.isTunnel(level)) {
            part = AssetRegistries.getPart(getRandomPart(hp.tunnel(bidirectional), info.chunkPos.x, info.chunkPos.z));
            if (part != null) generatePartForHighway(driver, info, part, transform, highwayGroundLevel, palette);
            shouldGenerateSupports = false; // Для туннелей опоры не нужны
        } else {
            if (info.isCity && level <= adj1.cityLevel && level <= adj2.cityLevel && adj1.isCity && adj2.isCity) {
                // Открытая магистраль в городе
                part = AssetRegistries.getPart(getRandomPart(hp.open(bidirectional), info.chunkPos.x, info.chunkPos.z));
                if (part != null) {
                    int height = generatePartForHighway(driver, info, part, transform, highwayGroundLevel, palette);
                    int clearHeight = 15;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            clearRange(driver, info, x, z, height, height + clearHeight, info.getWaterLevel() > info.groundLevel, Highways::isClearableAboveHighway);
                        }
                    }
                }
                shouldGenerateSupports = false; // Для открытых участков в городе опоры не нужны
            } else {
                // Мост - проверяем, нужны ли опоры
                shouldGenerateSupports = Highways.needsSupports(driver, highwayGroundLevel);
                ModLogger.debug("Bridge part at ({},{}), needs supports: {}", info.chunkPos.x, info.chunkPos.z, shouldGenerateSupports);
                
                part = AssetRegistries.getPart(getRandomPart(hp.bridge(bidirectional), info.chunkPos.x, info.chunkPos.z));
                if (part != null) {
                    int height = generatePartForHighway(driver, info, part, transform, highwayGroundLevel, palette);
                    int clearHeight = 15;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            clearRange(driver, info, x, z, height, height + clearHeight, info.getWaterLevel() > info.groundLevel, Highways::isClearableAboveHighway);
                        }
                    }
                }
            }
        }

        // Генерируем опоры только если это нужно
        if (part != null && shouldGenerateSupports && info.config.getActiveProfile().getHighwaySupports()) {
            Character supportChar = part.getMetaChar(META_SUPPORT);
            ModLogger.debug("Highway supports check - part: {}, supportChar: {}, supportsEnabled: {}", 
                part.getName(), supportChar, info.config.getActiveProfile().getHighwaySupports());
            if (supportChar != null) {
                BlockState sup = palette.get(supportChar.charValue(), net.minecraft.util.math.random.Random.create(0));
                ModLogger.debug("Support block state: {}", sup);
                if (sup != null) {
                    int x1 = transform.rotateX(0, 15), z1 = transform.rotateZ(0, 15);
                    int x2 = transform.rotateX(0, 0), z2 = transform.rotateZ(0, 0);
                    ModLogger.debug("Placing support columns at ({},{}) and ({},{}), Y={}", x1, z1, x2, z2, highwayGroundLevel - 1);
                    placeSupportColumn(driver, x1, highwayGroundLevel - 1, z1, sup);
                    placeSupportColumn(driver, x2, highwayGroundLevel - 1, z2, sup);
                }
            }
        }
    }

    private void placeSupportColumn(ChunkDriver driver, int lx, int startY, int lz, BlockState sup) {
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) return;
        int blocksPlaced = 0;
        for (int i = 0; i < 40; i++) {
            int y = startY - i;
            if (y < driver.world.getBottomY()) break;
            BlockState cur = driver.getBlock(lx, y, lz);
            if (!Highways.isEmpty(cur)) {
                ModLogger.debug("Support column at ({},{},{}) stopped at Y={} due to block: {}", lx, startY, lz, y, cur);
                break;
            }
            driver.setBlockNoNeighbors(lx, y, lz, sup);
            blocksPlaced++;
        }
        if (blocksPlaced > 0) {
            ModLogger.debug("Placed {} support blocks at ({},{},{})", blocksPlaced, lx, startY, lz);
        } else {
            ModLogger.debug("No support blocks placed at ({},{},{}) - ground already exists", lx, startY, lz);
        }
    }

    private String getRandomPart(java.util.List<String> parts, int chunkX, int chunkZ) {
        if (parts == null || parts.isEmpty()) return null;
        if (parts.size() == 1) return parts.get(0);
        long seed = chunkZ * 341873128712L + chunkX * 132897987541L;
        return parts.get(new java.util.Random(seed).nextInt(parts.size()));
    }

    /** clearRange с предикатом. Оригинал: LostCityTerrainFeature.clearRange. */
    private void clearRange(ChunkDriver driver, BuildingInfo info, int x, int z, int y1, int y2, boolean dowater, java.util.function.Predicate<BlockState> test) {
        if (dowater) {
            int wl = info.getWaterLevel();
            driver.fillColumn(x, z, y1, wl, Blocks.WATER.getDefaultState());
            driver.setBlockRangeToAir(x, wl + 1, z, y2, test);
        } else {
            driver.setBlockRangeToAir(x, y1, z, y2, test);
        }
    }

    /**
     * Генерация части для магистрали (WATERLEVEL: hard air -> water ниже waterLevel, иначе air).
     * Возвращает max Y размещённого блока.
     */
    private int generatePartForHighway(ChunkDriver driver, BuildingInfo info, BuildingPart part, Transform transform, int baseY, CompiledPalette palette) {
        int maxY = baseY;
        net.minecraft.util.math.random.Random rand = net.minecraft.util.math.random.Random.create(info.chunkPos.x * 341873128712L + info.chunkPos.z * 132897987541L);
        int waterLevel = info.getWaterLevel();
        for (int slice = 0; slice < part.getSliceCount(); slice++) {
            int y = baseY + slice;
            if (y < driver.world.getBottomY() || y >= driver.world.getTopY()) continue;
            for (int px = 0; px < part.getXSize() && px < 16; px++) {
                for (int pz = 0; pz < part.getZSize() && pz < 16; pz++) {
                    char c = part.getChar(px, slice, pz);
                    if (c == ' ' || c == '\0') continue;
                    BlockState state = palette.get(c, rand);
                    if (state == null) state = Blocks.STONE_BRICKS.getDefaultState();
                    if (state.getBlock() instanceof net.minecraft.block.StructureVoidBlock) {
                        state = y < waterLevel ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
                    }
                    // ВАЖНО: в оригинале ротация ВСЕГДА происходит относительно чанка 16x16, независимо от размера part!
                    int lx = transform.rotateX(px, pz);
                    int lz = transform.rotateZ(px, pz);
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    BlockState corrected = correctBlockState(state, lx, y, lz, driver);
                    if (corrected != null) driver.setBlock(lx, y, lz, corrected);
                    else driver.setBlock(lx, y, lz, state);
                    if (y > maxY) maxY = y;
                }
            }
        }
        return maxY;
    }

    /** Уличные лестницы на перепадах cityLevel. Оригинал: generateStreetDecorations. */
    private void generateStreetDecorations(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ) {
        BuildingInfo.StairDirection dir = info.getActualStairDirection();
        if (dir == null || info.stairType == null) return;
        BuildingPart part = AssetRegistries.getPart(info.stairType);
        if (part == null) return;
        Transform transform = switch (dir) {
            case XMIN -> Transform.ROTATE_NONE;
            case XMAX -> Transform.ROTATE_180;
            case ZMIN -> Transform.ROTATE_90;
            case ZMAX -> Transform.ROTATE_270;
        };
        int oy = info.getCityGroundLevel() + 1;
        CompiledPalette streetPalette = AssetRegistries.getStreetPalette();
        if (streetPalette == null) return;
        long seed = (driver.world != null ? driver.world.getSeed() : 0) + (long) info.chunkPos.z * 341873128712L + (long) info.chunkPos.x * 132897987541L;
        generatePartForStreet(driver, part, transform, oy, streetPalette, seed);
    }
    
    /**
     * Генерация одной колонки (столбца блоков).
     * Для улиц/парков: платформа из камня/кирпича как в оригинале; для зданий — травка+земля.
     */
    private void generateColumn(ChunkDriver driver, BuildingInfo info, int localX, int localZ, int groundLevel) {
        int originalHeight = driver.getHeight(localX, localZ);
        boolean isStreet = info.isStreetOrParkSection();
        net.minecraft.block.BlockState fillBlock = isStreet ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState();
        // В оригинале "пустые" места в street-партах (символ b=structure_void) оставляют то, что уже есть.
        // Чтобы это выглядело как тротуар/плитка, а не грязь — используем stone_bricks как базовую поверхность улицы.
        net.minecraft.block.BlockState surfaceBlock = isStreet ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();

        if (originalHeight > groundLevel) {
            for (int y = groundLevel + 1; y <= originalHeight; y++) {
                driver.setBlockNoNeighbors(localX, y, localZ, Blocks.AIR.getDefaultState());
            }
            driver.setBlock(localX, groundLevel, localZ, surfaceBlock);
            for (int y = groundLevel - 1; y >= groundLevel - 3 && y >= driver.world.getBottomY(); y--) {
                driver.setBlockNoNeighbors(localX, y, localZ, fillBlock);
            }
        } else if (originalHeight < groundLevel) {
            for (int y = originalHeight + 1; y < groundLevel; y++) {
                driver.setBlockNoNeighbors(localX, y, localZ, fillBlock);
            }
            driver.setBlock(localX, groundLevel, localZ, surfaceBlock);
        } else {
            driver.setBlock(localX, groundLevel, localZ, surfaceBlock);
        }
    }

    /**
     * Заполнить пустоту под улицей до бедрока камнем (как в оригинале fillToBedrockStreetBlock).
     * Только для чанков улиц/парков; вызывается перед generateStreet.
     */
    private void fillToBedrockStreetBlock(ChunkDriver driver, BuildingInfo info) {
        int groundLevel = info.getCityGroundLevel();
        int minY = driver.world.getBottomY();
        BlockState base = Blocks.STONE.getDefaultState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int y = groundLevel - 1;
                while (y > minY && Highways.isEmpty(driver.getBlock(x, y, z))) {
                    driver.setBlockNoNeighbors(x, y, z, base);
                    y--;
                }
            }
        }
    }

    /** Направление границы чанка (для заборов). Оригинал: Direction. */
    private enum BorderDir {
        XMIN, XMAX, ZMIN, ZMAX;
        static final BorderDir[] VALUES = { XMIN, XMAX, ZMIN, ZMAX };
        boolean atSide(int x, int z) {
            return switch (this) {
                case XMIN -> x == 0;
                case XMAX -> x == 15;
                case ZMIN -> z == 0;
                case ZMAX -> z == 15;
            };
        }
        BorderDir getOpposite() {
            return switch (this) {
                case XMIN -> XMAX;
                case XMAX -> XMIN;
                case ZMIN -> ZMAX;
                case ZMAX -> ZMIN;
            };
        }
        Transform getRotation() {
            return switch (this) {
                case XMIN -> Transform.ROTATE_NONE;
                case XMAX -> Transform.ROTATE_180;
                case ZMIN -> Transform.ROTATE_90;
                case ZMAX -> Transform.ROTATE_270;
            };
        }
        BuildingInfo getAdjacent(BuildingInfo info) {
            return switch (this) {
                case XMIN -> info.getXmin();
                case XMAX -> info.getXmax();
                case ZMIN -> info.getZmin();
                case ZMAX -> info.getZmax();
            };
        }
        boolean isX() { return this == XMIN || this == XMAX; }
    }

    /**
     * Генерация границ (заборы/столбы) по краям улиц, как в оригинале generateBorders.
     * Использует border (y) и wall (w) из street palette.
     */
    private void generateBorders(ChunkDriver driver, BuildingInfo info) {
        CompiledPalette palette = AssetRegistries.getStreetPalette();
        if (palette == null) return;
        char borderChar = 'y';
        char wallChar = 'w';
        int ground = info.getCityGroundLevel();
        long seed = (driver.world != null ? driver.world.getSeed() : 0) + (long) info.chunkPos.z * 341873128712L + (long) info.chunkPos.x * 132897987541L;
        var rand = net.minecraft.util.math.random.Random.create(seed);
        boolean canDoParks = true;

        if (doBorder(info, BorderDir.XMIN)) {
            for (int z = 0; z < 16; z++) generateBorder(driver, info, 0, z, info.getXmin(), ground, palette, borderChar, wallChar, canDoParks, rand);
        }
        if (doBorder(info, BorderDir.XMAX)) {
            for (int z = 0; z < 16; z++) generateBorder(driver, info, 15, z, info.getXmax(), ground, palette, borderChar, wallChar, canDoParks, rand);
        }
        if (doBorder(info, BorderDir.ZMIN)) {
            for (int x = 0; x < 16; x++) generateBorder(driver, info, x, 0, info.getZmin(), ground, palette, borderChar, wallChar, canDoParks, rand);
        }
        if (doBorder(info, BorderDir.ZMAX)) {
            for (int x = 0; x < 16; x++) generateBorder(driver, info, x, 15, info.getZmax(), ground, palette, borderChar, wallChar, canDoParks, rand);
        }
    }

    private boolean doBorder(BuildingInfo info, BorderDir dir) {
        BuildingInfo adj = switch (dir) { case XMIN -> info.getXmin(); case XMAX -> info.getXmax(); case ZMIN -> info.getZmin(); case ZMAX -> info.getZmax(); };
        if (isHigherThanNearbyStreetChunk(info, adj)) return true;
        if (!adj.isCity && adj.cityLevel <= info.cityLevel) return true;
        return false;
    }

    private boolean isHigherThanNearbyStreetChunk(BuildingInfo info, BuildingInfo adjacent) {
        if (!adjacent.isCity) return false;
        if (adjacent.hasBuilding) return adjacent.cityLevel + adjacent.getNumFloors() < info.cityLevel;
        return adjacent.cityLevel < info.cityLevel;
    }

    private void generateBorder(ChunkDriver driver, BuildingInfo info, int x, int z, BuildingInfo adjacent,
            int ground, CompiledPalette palette, char borderChar, char wallChar, boolean canDoParks,
            net.minecraft.util.math.random.Random rand) {
        int y = driver.getHeight(x, z);
        int yLo, yHi;
        if (y < ground + 1) {
            yLo = y - 1;
            yHi = ground + 1;
        } else {
            yLo = ground - 3;
            yHi = ground + 1;
        }
        yLo = Math.max(yLo, driver.world.getBottomY());
        setBlocksFromPalette(driver, x, yLo, z, yHi, palette, borderChar, rand);
        if (canDoParks && !borderNeedsConnectionToAdjacent(info, x, z)) {
            BlockState wall = palette.get(wallChar, rand);
            // Важно: у стен соединения хранятся в BlockState. Если поставить "сырой" state — будут одиночные столбики,
            // и соединения появятся только после ручного обновления (сломать/поставить).
            if (wall != null) {
                BlockState corrected = correctBlockState(wall, x, ground + 1, z, driver);
                if (corrected != null) driver.setBlock(x, ground + 1, z, corrected);
            }
        }
    }

    /**
     * Оригинал: borderNeedsConnectionToAdjacentChunk.
     * Цикл по ВСЕМ границам, на которых лежит (x,z): углы (0,0), (15,15) и т.д. учитываются дважды.
     * Мост: не ставим wall на всей границе. Лестница: только в зоне z1–z2.
     */
    private boolean borderNeedsConnectionToAdjacent(BuildingInfo info, int x, int z) {
        for (BorderDir dir : BorderDir.VALUES) {
            if (!dir.atSide(x, z)) continue;
            BuildingInfo adj = dir.getAdjacent(info);
            if (adj == null) continue;
            if (dir.isX() && adj.hasXBridge() != null) return true;
            if (!dir.isX() && adj.hasZBridge() != null) return true;
            BuildingInfo.StairDirection opposite = switch (dir.getOpposite()) {
                case XMIN -> BuildingInfo.StairDirection.XMIN;
                case XMAX -> BuildingInfo.StairDirection.XMAX;
                case ZMIN -> BuildingInfo.StairDirection.ZMIN;
                case ZMAX -> BuildingInfo.StairDirection.ZMAX;
            };
            if (adj.getActualStairDirection() != opposite) continue;
            BuildingPart stairType = adj.stairType != null ? AssetRegistries.getPart(adj.stairType) : null;
            if (stairType == null) continue;
            Integer z1 = stairType.getMetaInteger("z1");
            Integer z2 = stairType.getMetaInteger("z2");
            if (z1 == null || z2 == null) continue;
            Transform transform = dir.getOpposite().getRotation();
            int xx1 = transform.rotateX(15, z1);
            int zz1 = transform.rotateZ(15, z1);
            int xx2 = transform.rotateX(15, z2);
            int zz2 = transform.rotateZ(15, z2);
            if (x >= Math.min(xx1, xx2) && x <= Math.max(xx1, xx2) && z >= Math.min(zz1, zz2) && z <= Math.max(zz1, zz2))
                return true;
        }
        return false;
    }

    private void setBlocksFromPalette(ChunkDriver driver, int x, int yLo, int z, int yHi, CompiledPalette palette, char c,
            net.minecraft.util.math.random.Random rand) {
        for (int y = yLo; y <= yHi; y++) {
            if (y < driver.world.getBottomY() || y >= driver.world.getTopY()) continue;
            BlockState s = palette.get(c, rand);
            if (s != null) driver.setBlockNoNeighbors(x, y, z, s);
        }
    }

    /** Лианы на внешних стенах зданий (оригинал: ChunkFixer.generateVines). */
    private void generateVines(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ) {
        float vineChance = info.config.getActiveProfile().vineChance;
        if (vineChance < 0.000001f) return;
        int ground = info.getCityGroundLevel();
        int maxHeight = ground + info.getBuildingHeight(); // Максимальная высота здания
        int fromY = Math.max(driver.world.getBottomY() + 1, ground + 3);
        // Не выше здания!
        int toY = Math.min(maxHeight, driver.world.getTopY() - 1);
        if (fromY >= toY) return;
        
        java.util.Random r = new java.util.Random(chunkZ * 341873128712L + chunkX * 132897987541L);
        net.minecraft.block.BlockState vineWest = net.minecraft.block.Blocks.VINE.getDefaultState().with(net.minecraft.block.VineBlock.WEST, true);
        net.minecraft.block.BlockState vineEast = net.minecraft.block.Blocks.VINE.getDefaultState().with(net.minecraft.block.VineBlock.EAST, true);
        net.minecraft.block.BlockState vineNorth = net.minecraft.block.Blocks.VINE.getDefaultState().with(net.minecraft.block.VineBlock.NORTH, true);
        net.minecraft.block.BlockState vineSouth = net.minecraft.block.Blocks.VINE.getDefaultState().with(net.minecraft.block.VineBlock.SOUTH, true);
        
        // Только на краях, где есть стена (проверяем что блок не воздух)
        for (int z = 1; z < 15; z++) {
            for (int y = fromY; y < toY; y++) {
                if (r.nextFloat() < vineChance) {
                    // Проверяем что есть стена для лианы
                    if (!driver.getBlock(0, y, z).isAir()) {
                        driver.setBlockNoNeighbors(0, y, z, vineWest);
                    }
                }
                if (r.nextFloat() < vineChance) {
                    if (!driver.getBlock(15, y, z).isAir()) {
                        driver.setBlockNoNeighbors(15, y, z, vineEast);
                    }
                }
            }
        }
        for (int x = 1; x < 15; x++) {
            for (int y = fromY; y < toY; y++) {
                if (r.nextFloat() < vineChance) {
                    if (!driver.getBlock(x, y, 0).isAir()) {
                        driver.setBlockNoNeighbors(x, y, 0, vineNorth);
                    }
                }
                if (r.nextFloat() < vineChance) {
                    if (!driver.getBlock(x, y, 15).isAir()) {
                        driver.setBlockNoNeighbors(x, y, 15, vineSouth);
                    }
                }
            }
        }
    }

    /** Листья на границе улица/здание (оригинал: CHANCE_OF_RANDOM_LEAFBLOCKS). Только там, где есть соседнее здание. */
    private void generateLeavesForStreet(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ) {
        float chance = info.config.getActiveProfile().chanceOfRandomLeafBlocks;
        if (chance < 0.000001f) return;
        int ground = info.getCityGroundLevel();
        int thick = Math.max(1, Math.min(8, info.config.getActiveProfile().thicknessOfRandomLeafBlocks));
        java.util.Random r = new java.util.Random(chunkZ * 132897987541L + chunkX * 341873128712L);
        net.minecraft.block.BlockState leaf = net.minecraft.block.Blocks.OAK_LEAVES.getDefaultState();
        
        // Только на границе с зданиями (оригинал: info.getXmin().hasBuilding и т.д.)
        BuildingInfo xmin = info.getXmin();
        BuildingInfo xmax = info.getXmax();
        BuildingInfo zmin = info.getZmin();
        BuildingInfo zmax = info.getZmax();
        
        // Запад (x=0): только если xmin имеет здание
        if (xmin != null && xmin.hasBuilding) {
            for (int x = 0; x < thick; x++) {
                float v = Math.min(0.8f, chance * (thick + 1 - x));
                for (int z = 0; z < 16; z++) {
                    if (r.nextFloat() >= v) continue;
                    // Оригинал: идёт вниз до не-воздуха, затем ставит листья
                    int y = ground;
                    while (y > 0 && driver.getBlock(x, y, z).isAir()) y--;
                    for (int ly = Math.max(y, ground); ly <= ground + 4 && ly < driver.world.getTopY(); ly++) {
                        if (driver.getBlock(x, ly, z).isAir()) driver.setBlockNoNeighbors(x, ly, z, leaf);
                    }
                }
            }
        }
        
        // Восток (x=15): только если xmax имеет здание
        if (xmax != null && xmax.hasBuilding) {
            for (int x = 16 - thick; x < 16; x++) {
                float v = Math.min(0.8f, chance * (x - 15 + thick));
                for (int z = 0; z < 16; z++) {
                    if (r.nextFloat() >= v) continue;
                    int y = ground;
                    while (y > 0 && driver.getBlock(x, y, z).isAir()) y--;
                    for (int ly = Math.max(y, ground); ly <= ground + 4 && ly < driver.world.getTopY(); ly++) {
                        if (driver.getBlock(x, ly, z).isAir()) driver.setBlockNoNeighbors(x, ly, z, leaf);
                    }
                }
            }
        }
        
        // Север (z=0): только если zmin имеет здание
        if (zmin != null && zmin.hasBuilding) {
            for (int z = 0; z < thick; z++) {
                float v = Math.min(0.8f, chance * (thick + 1 - z));
                for (int x = thick; x < 16 - thick; x++) {
                    if (r.nextFloat() >= v) continue;
                    int y = ground;
                    while (y > 0 && driver.getBlock(x, y, z).isAir()) y--;
                    for (int ly = Math.max(y, ground); ly <= ground + 4 && ly < driver.world.getTopY(); ly++) {
                        if (driver.getBlock(x, ly, z).isAir()) driver.setBlockNoNeighbors(x, ly, z, leaf);
                    }
                }
            }
        }
        
        // Юг (z=15): только если zmax имеет здание
        if (zmax != null && zmax.hasBuilding) {
            for (int z = 16 - thick; z < 16; z++) {
                float v = Math.min(0.8f, chance * (z - 15 + thick));
                for (int x = thick; x < 16 - thick; x++) {
                    if (r.nextFloat() >= v) continue;
                    int y = ground;
                    while (y > 0 && driver.getBlock(x, y, z).isAir()) y--;
                    for (int ly = Math.max(y, ground); ly <= ground + 4 && ly < driver.world.getTopY(); ly++) {
                        if (driver.getBlock(x, ly, z).isAir()) driver.setBlockNoNeighbors(x, ly, z, leaf);
                    }
                }
            }
        }
    }

    /**
     * Генерация зданий в чанке
     */
    private void generateBuildings(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ) {
        Building building = AssetRegistries.getBuilding(info.buildingType);
        if (building == null) {
            ModLogger.warn("Building '{}' not found for chunk ({}, {})", info.buildingType, chunkX, chunkZ);
            return;
        }
        
        // Этап 3.1: Палитра через CityStyle -> Style.getRandomPalette(). Оригинал: topleft.getCompiledPalette() для multi.
        Palette stylePalette = null;
        LostCityConfig config = info.config;
        int paletteChunkX = chunkX;
        int paletteChunkZ = chunkZ;
        Building paletteBuilding = building;
        if (info.multiBuildingPos != null && info.multiBuildingPos.isMulti() && !info.multiBuildingPos.isTopLeft() && info.multiBuilding != null) {
            paletteChunkX = chunkX - info.multiBuildingPos.x();
            paletteChunkZ = chunkZ - info.multiBuildingPos.z();
            String tlPart = info.multiBuilding.getBuilding(0, 0);
            if (tlPart != null) {
                Building tlB = AssetRegistries.getBuilding(tlPart);
                if (tlB != null) paletteBuilding = tlB;
            }
        }
        if (info.isCity) {
            com.lostcity.assets.CityStyle cityStyle = City.getCityStyle(new ChunkPos(paletteChunkX, paletteChunkZ), config, driver.world);
            if (cityStyle != null && cityStyle.getStyle() != null) {
                com.lostcity.assets.Style style = AssetRegistries.getStyle(cityStyle.getStyle());
                if (style != null) {
                    java.util.Random styleRand = new java.util.Random(
                        (long) paletteChunkX * 341873128712L + (long) paletteChunkZ * 132897987541L);
                    stylePalette = style.getRandomPalette(driver.world, styleRand);
                    if (stylePalette != null && callCount <= 5) {
                        ModLogger.debug("Style palette from CityStyle '{}' -> Style '{}': {} entries",
                            cityStyle.getId(), cityStyle.getStyle(), stylePalette.size());
                    }
                }
            }
        } else {
            // Негородской чанк - используем outside style (упрощенная версия)
            // В оригинале: getOutsideStyle() -> WorldStyle.getOutsideStyle()
            // Для упрощения используем стандартный Style
            com.lostcity.assets.Style outsideStyle = AssetRegistries.getStyle("lostcities:outside");
            if (outsideStyle != null) {
                java.util.Random styleRand = new java.util.Random(
                    (long) paletteChunkX * 341873128712L + (long) paletteChunkZ * 132897987541L);
                stylePalette = outsideStyle.getRandomPalette(driver.world, styleRand);
            }
        }
        
        // Fallback для всех зданий (включая мульти-здания, если top-left еще не сгенерирован)
        if (stylePalette == null) {
            if (info.multiBuildingPos != null && info.multiBuildingPos.isMulti() && !info.multiBuildingPos.isTopLeft()) {
                ModLogger.warn("Multi part ({}, {}): no style palette from top-left ({}, {}). Using fallback.", chunkX, chunkZ, paletteChunkX, paletteChunkZ);
            }
            Palette commonPalette = AssetRegistries.getPalette("lostcities:common");
            Palette defaultPalette = AssetRegistries.getPalette("lostcities:default");
            
            if (commonPalette == null && defaultPalette == null) {
                ModLogger.error("No base palettes (common/default) found! Available palettes: {}", 
                    AssetRegistries.getPaletteCount());
                return;
            }
            
            List<Palette> basePalettes = new ArrayList<>();
            if (commonPalette != null) basePalettes.add(commonPalette);
            if (defaultPalette != null) basePalettes.add(defaultPalette);
            stylePalette = basePalettes.isEmpty() ? null : basePalettes.get(0);
            if (basePalettes.size() > 1) {
                // Объединяем палитры
                for (int i = 1; i < basePalettes.size(); i++) {
                    stylePalette.merge(basePalettes.get(i));
                }
            }
        }
        
        if (stylePalette == null) {
            ModLogger.error("Failed to get style palette for chunk ({}, {})", chunkX, chunkZ);
            return;
        }
        
        CompiledPalette compiledPalette = new CompiledPalette(stylePalette);
        if (paletteBuilding.getRefPaletteName() != null) {
            Palette buildingPalette = AssetRegistries.getPalette(paletteBuilding.getRefPaletteName());
            if (buildingPalette != null) {
                compiledPalette = new CompiledPalette(compiledPalette, buildingPalette);
                if (callCount <= 5) {
                    ModLogger.info("Merged building palette '{}' ({} symbols) into compiled palette", 
                        paletteBuilding.getRefPaletteName(), buildingPalette.size());
                }
            } else {
                ModLogger.warn("Building palette '{}' not found for building '{}'", 
                    paletteBuilding.getRefPaletteName(), paletteBuilding.getName());
            }
        }
        
        if (callCount <= 5) {
            ModLogger.info("Style palette: {} symbols, compiled: {} symbols", 
                stylePalette != null ? stylePalette.size() : 0,
                compiledPalette.getCharacters().size());
            
            if (compiledPalette.getCharacters().size() == 0) {
                ModLogger.error("ERROR: CompiledPalette is EMPTY! Buildings will not generate correctly!");
            }
            
            // Логируем первые несколько символов для отладки
            Set<Character> chars = compiledPalette.getCharacters();
            List<Character> charList = new ArrayList<>(chars);
            Collections.sort(charList);
            int logCount = Math.min(30, charList.size());
            ModLogger.info("Sample symbols in compiled palette (first {}): {}", 
                logCount, charList.subList(0, logCount));
        }
        
        // Оригинал НЕ использует ротацию зданий! Всегда ROTATE_NONE (строка 2317: Transform.ROTATE_NONE)
        Transform buildingRotation = Transform.ROTATE_NONE;

        // === Make room for building (как в оригинале) ===
        // Критично для:
        // - зданий “в земле” (не убрали террейн внутри)
        // - “фантомных” дверей/проёмов (нет filler/бордера на уровне lowestLevel)
        int lowestLevel = info.getYForFloor(-info.cellars);
        makeRoomForBuilding(driver, info, lowestLevel, compiledPalette, building);

        if (ModLogger.isDebugEnabled()) {
            ModLogger.debug("=== GENERATING BUILDING ===");
            ModLogger.debug("Chunk: ({}, {})", chunkX, chunkZ);
            ModLogger.debug("Building: {}, rotation: {}", info.buildingType, buildingRotation);
            ModLogger.info("Floors: {}, Cellars: {}", info.floors, info.cellars);
            ModLogger.info("Ground level: {}", info.groundLevel);
            ModLogger.info("MultiPos: {}, MultiBuilding: {}", info.multiBuildingPos, info.multiBuilding != null ? info.multiBuilding.getName() : "null");
            ModLogger.info("Available parts: {}", building.getParts().size());
        }

        int seedX = info.multiBuildingPos.isMulti() ? (info.chunkPos.x - info.multiBuildingPos.x()) : info.chunkPos.x;
        int seedZ = info.multiBuildingPos.isMulti() ? (info.chunkPos.z - info.multiBuildingPos.z()) : info.chunkPos.z;
        java.util.Random part2Rand = new java.util.Random((long) seedX * 31L + (long) seedZ * 17L + 7L);

        for (int floor = -info.cellars; floor <= info.floors; floor++) {
            generateFloor(driver, building, compiledPalette, info, floor, chunkX, chunkZ, buildingRotation);
            boolean isTop = (floor == info.floors);
            // Оригинал: только !isTop && info.getAllowDoors(). Без проверки connectionAtX != null.
            if (!isTop && building.getAllowDoors()) {
                int baseY = info.getYForFloor(floor);
                Doors.generateDoors(driver, info, baseY + 1, floor, compiledPalette);
            }
            if (!building.getParts2().isEmpty() && floor > -info.cellars) {
                final int currentFloor = floor;
                List<Building.PartRef> matching = building.getParts2().stream()
                    .filter(pr -> pr.isValidForFloor(currentFloor, isTop)).toList();
                if (!matching.isEmpty()) {
                    Building.PartRef pr2 = matching.get(part2Rand.nextInt(matching.size()));
                    BuildingPart part2 = resolvePart(pr2.part);
                    if (part2 != null) {
                        int baseY = info.getYForFloor(floor);
                        placePart2At(driver, part2, baseY, compiledPalette, building, info, buildingRotation);
                    }
                }
            }
        }

        // Отверстия над лестницами — только ПОСЛЕ генерации всех этажей. Иначе потолок (пол след. этажа)
        // ставится после нашей очистки и снова заделывает проём.
        for (int floor = -info.cellars; floor <= info.floors - 1; floor++) {
            createStairOpenings(driver, info, floor, buildingRotation);
        }
        
        if (info.cellars >= 1) {
            Corridors.generateCorridorConnections(driver, info);
        }
        
        if (callCount <= 5) {
            ModLogger.info("Building generation complete for chunk ({}, {})", chunkX, chunkZ);
        }
    }
    
    /**
     * Генерация одного этажа здания. Ротация здания (оригинал: Transform.randomRotation) применяется к части.
     */
    private void generateFloor(ChunkDriver driver, Building building, CompiledPalette compiledPalette,
                               BuildingInfo info, int floorIndex, int chunkX, int chunkZ, Transform buildingRotation) {
        boolean isTopFloor = (floorIndex == info.floors);
        Building.PartRef partRef = null;
        
        List<Building.PartRef> validParts = new ArrayList<>();
        for (Building.PartRef pr : building.getParts()) {
            if (pr.isValidForFloor(floorIndex, isTopFloor)) {
                validParts.add(pr);
            }
        }
        
        if (!validParts.isEmpty()) {
            int seedX = info.multiBuildingPos.isMulti() ? (info.chunkPos.x - info.multiBuildingPos.x()) : info.chunkPos.x;
            int seedZ = info.multiBuildingPos.isMulti() ? (info.chunkPos.z - info.multiBuildingPos.z()) : info.chunkPos.z;
            net.minecraft.util.math.random.Random floorRand = net.minecraft.util.math.random.Random.create((long) seedX * 341873128712L + (long) seedZ * 132897987541L + floorIndex * 17L);
            partRef = validParts.get(floorRand.nextInt(validParts.size()));
        } else {
            for (Building.PartRef pr : building.getParts()) {
                if (pr.top == isTopFloor) {
                    partRef = pr;
                    break;
                }
            }
        }
        
        // Если не нашли точное совпадение, используем любую доступную часть
        if (partRef == null && !building.getParts().isEmpty()) {
            partRef = building.getParts().get(0);
            ModLogger.debug("No exact part match for floor {} (top={}), using first available part", 
                floorIndex, isTopFloor);
        }
        
        if (partRef == null) {
            ModLogger.warn("No part found for floor {} in building {} (chunk {}, {})", 
                floorIndex, building.getName(), chunkX, chunkZ);
            return;
        }
        
        // Загружаем BuildingPart
        BuildingPart part = AssetRegistries.getPart(partRef.part);
        if (part == null) {
            // Пробуем с namespace
            part = AssetRegistries.getPart("lostcities:" + partRef.part);
        }
        if (part == null) {
            // Пробуем без namespace, если уже был
            String partName = partRef.part;
            if (partName.contains(":")) {
                part = AssetRegistries.getPart(partName.split(":")[1]);
            }
        }
        if (part == null) {
            ModLogger.warn("Part '{}' not found for building '{}' (floor {})", 
                partRef.part, building.getName(), floorIndex);
            return;
        }
        
        // Получаем CompiledPalette для части (может быть refpalette)
        CompiledPalette partCompiledPalette = compiledPalette;
        if (part.getRefPaletteName() != null) {
            Palette refPal = AssetRegistries.getPalette(part.getRefPaletteName());
            if (refPal != null) {
                partCompiledPalette = new CompiledPalette(compiledPalette, refPal);
            }
        }
        
        // Y координата для этого этажа
        int baseY = info.getYForFloor(floorIndex);
        
        // Размещаем блоки по срезам
        int blocksPlaced = 0;
        int blocksSkipped = 0;
        int missingSymbols = 0;
        Set<Character> missingChars = new HashSet<>();
        
        int sx = part.getXSize();
        int sz = part.getZSize();
        for (int slice = 0; slice < part.getSliceCount(); slice++) {
            int y = baseY + slice;
            if (y < driver.world.getBottomY() || y >= driver.world.getTopY()) continue;

            for (int px = 0; px < sx && px < 16; px++) {
                for (int pz = 0; pz < sz && pz < 16; pz++) {
                    char c = part.getChar(px, slice, pz);
                    if (c == ' ' || c == '\0') continue;

                    // Оригинал НЕ использует ротацию зданий, координаты остаются как есть
                    int lx = px;
                    int lz = pz;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;

                    BlockState state = partCompiledPalette.get(c);
                    if (state == null) {
                        char fillCh = building.getFiller();
                        if (fillCh != '\0') state = partCompiledPalette.get(fillCh);
                        if (state == null) state = Blocks.STONE_BRICKS.getDefaultState();
                        if (state == null) {
                            missingSymbols++;
                            missingChars.add(c);
                            blocksSkipped++;
                            if (callCount <= 10 || missingSymbols <= 5) {
                                ModLogger.warn("Symbol '{}' (0x{}) not found in palette for building '{}', part '{}', floor {} (filler: '{}')",
                                    c, Integer.toHexString((int) c & 0xFF), building.getName(), part.getName(), floorIndex, fillCh);
                            }
                            continue;
                        } else if (callCount <= 5 && missingSymbols == 0) {
                            ModLogger.debug("Symbol '{}' not found, using filler '{}' or default", c, fillCh);
                        }
                    }
                    // Оригинал НЕ использует ротацию зданий (всегда ROTATE_NONE), но применяет ротацию к некоторым блокам через теги
                    // У нас ротация отключена для зданий

                    BlockState correctedState = correctBlockState(state, lx, y, lz, driver);
                    if (correctedState == null) continue;

                    driver.setBlock(lx, y, lz, correctedState);
                    blocksPlaced++;

                    // Сундуки с лутом (оригинал: handleLoot)
                    if (!info.noLoot) {
                        String lootCond = partCompiledPalette.getLoot(c);
                        if (lootCond != null && !lootCond.isBlank() && info.config.getActiveProfile().getGenerateLoot()) {
                            if (correctedState.getBlock() == Blocks.CHEST || correctedState.getBlock() == Blocks.TRAPPED_CHEST || correctedState.getBlock() == Blocks.BARREL) {
                                BlockPos pos = driver.getBlockPos(lx, y, lz);
                                Identifier lootId = "chestloot".equals(lootCond)
                                    ? Identifier.of(LostCityMod.MOD_ID, "chests/lostcitychest")
                                    : lootCond.contains(":") ? Identifier.tryParse(lootCond) : Identifier.of(LostCityMod.MOD_ID, lootCond);
                                if (lootId != null) {
                                    info.addPostTodo(() -> {
                                        var be = driver.world.getBlockEntity(pos);
                                        if (be instanceof LootableContainerBlockEntity lootable) {
                                            long seed = (long) info.chunkPos.x * 341873128712L + info.chunkPos.z * 132897987541L + pos.getX() * 31L + pos.getZ() * 17L + pos.getY();
                                            // В 1.20.5 таблицы лута переехали в реестр:
                                            // setLootTable(Identifier, long) -> setLootTable(RegistryKey) + setLootTableSeed(long)
                                            //? if >=1.20.5 {
                                            /*lootable.setLootTable(net.minecraft.registry.RegistryKey.of(
                                                    net.minecraft.registry.RegistryKeys.LOOT_TABLE, lootId));
                                            lootable.setLootTableSeed(seed);
                                            *///?} else
                                            lootable.setLootTable(lootId, seed);
                                        }
                                    });
                                }
                            }
                        }

                        // Спавнеры мобов (оригинал: handleSpawner)
                        String mobId = partCompiledPalette.getMobId(c);
                        boolean isSpawnerBlock = correctedState.getBlock() == Blocks.SPAWNER;
                        if ((mobId != null && !mobId.isBlank()) || isSpawnerBlock) {
                            if (info.config.getActiveProfile().getGenerateSpawners()) {
                                final String finalMobId = (mobId != null && !mobId.isBlank()) ? mobId : "minecraft:zombie";
                                BlockPos pos = driver.getBlockPos(lx, y, lz);
                                info.addPostTodo(() -> {
                                    var be = driver.world.getBlockEntity(pos);
                                    if (be instanceof net.minecraft.block.entity.MobSpawnerBlockEntity spawner) {
                                        String mobString = finalMobId.contains(":") ? finalMobId : "minecraft:" + finalMobId;
                                        // Раньше моб задавался ручной сборкой NBT и spawner.readNbt().
                                        // В 1.20.5 readNbt/createNbt стали принимать WrapperLookup и ушли
                                        // в protected. setEntityType — публичный API с одинаковой
                                        // подписью во всех поддерживаемых версиях.
                                        Identifier mobIdent = Identifier.tryParse(mobString);
                                        net.minecraft.entity.EntityType<?> type = mobIdent == null ? null
                                                : net.minecraft.registry.Registries.ENTITY_TYPE.get(mobIdent);
                                        if (type != null) {
                                            spawner.setEntityType(type,
                                                    net.minecraft.util.math.random.Random.create(pos.asLong()));
                                        }
                                    }
                                });
                            } else if (isSpawnerBlock) {
                                driver.setBlock(lx, y, lz, Blocks.AIR.getDefaultState());
                            }
                        }
                    } else if (correctedState.getBlock() == Blocks.SPAWNER) {
                        // Если noLoot=true, удаляем спавнеры
                        driver.setBlock(lx, y, lz, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        if (callCount <= 5 || !missingChars.isEmpty()) {
            ModLogger.info("Floor {}: part '{}', {} blocks placed, {} skipped", 
                floorIndex, part.getName(), blocksPlaced, blocksSkipped);
            if (!missingChars.isEmpty()) {
                ModLogger.warn("Missing symbols in palette for building '{}', part '{}', floor {}: {} symbols (chars: {})", 
                    building.getName(), part.getName(), floorIndex, missingSymbols, missingChars);
                
                // Показываем, какие символы есть в палитре для сравнения
                Set<Character> availableChars = partCompiledPalette.getCharacters();
                ModLogger.warn("Available symbols in palette: {} total. Sample: {}", 
                    availableChars.size(), 
                    availableChars.stream().limit(20).toList());
            }
        }
    }

    private static BuildingPart resolvePart(String name) {
        BuildingPart p = AssetRegistries.getPart(name);
        if (p != null) return p;
        p = AssetRegistries.getPart("lostcities:" + name);
        if (p != null) return p;
        if (name != null && name.contains(":")) {
            return AssetRegistries.getPart(name.split(":")[1]);
        }
        return null;
    }

    /**
     * Генерация part2 (внутренние лестницы) на этаже. Оригинал: generatePart для part2Map.
     */
    private void placePart2At(ChunkDriver driver, BuildingPart part, int baseY,
                              CompiledPalette compiledPalette, Building building, BuildingInfo info, Transform rot) {
        CompiledPalette pal = compiledPalette;
        if (part.getRefPaletteName() != null) {
            Palette ref = AssetRegistries.getPalette(part.getRefPaletteName());
            if (ref != null) pal = new CompiledPalette(compiledPalette, ref);
        }
        int sx = part.getXSize();
        int sz = part.getZSize();
        for (int slice = 0; slice < part.getSliceCount(); slice++) {
            int y = baseY + slice;
            if (y < driver.world.getBottomY() || y >= driver.world.getTopY()) continue;
            for (int px = 0; px < sx && px < 16; px++) {
                for (int pz = 0; pz < sz && pz < 16; pz++) {
                    char c = part.getChar(px, slice, pz);
                    if (c == ' ' || c == '\0') continue;
                    int lx = px;
                    int lz = pz;
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    BlockState state = pal.get(c);
                    if (state == null) {
                        state = pal.get(building.getFiller());
                        if (state == null) state = Blocks.STONE_BRICKS.getDefaultState();
                    }
                    if (state == null) continue;
                    BlockState corrected = correctBlockState(state, lx, y, lz, driver);
                    if (corrected != null) driver.setBlock(lx, y, lz, corrected);
                }
            }
        }
    }

    /**
     * Make room for building (упрощённо, но по сути как Forge makeRoomForBuilding для DEFAULT/SHPERES).
     * - На краях делает тонкий “бордер” до lowestLevel
     * - Гарантирует, что на lowestLevel не воздух (ставит filler)
     * - Чистит внутренности здания от террейна, чтобы не было “здания в земле”
     */
    private void makeRoomForBuilding(ChunkDriver driver, BuildingInfo info, int lowestLevel, CompiledPalette palette, Building building) {
        if (!info.hasBuilding) return;
        int topClear = info.getCityGroundLevel() + info.getNumFloors() * FLOOR_HEIGHT; // до крыши
        topClear = Math.min(topClear, driver.world.getTopY() - 1);

        char borderChar = 'y'; // стандартный border из citystyle_common
        BlockState filler = palette.get(building.getFiller());
        if (filler == null) filler = Blocks.STONE_BRICKS.getDefaultState();

        net.minecraft.util.math.random.Random rand = net.minecraft.util.math.random.Random.create(
            info.chunkPos.x * 341873128712L + info.chunkPos.z * 132897987541L
        );

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean side = (x == 0 || x == 15 || z == 0 || z == 15);
                if (side) {
                    int y = driver.getHeight(x, z);
                    if (y >= lowestLevel) y = lowestLevel - 3;
                    y = Math.max(y, driver.world.getBottomY());
                    // бордер до нижнего уровня здания
                    for (int yy = y; yy <= lowestLevel; yy++) {
                        BlockState b = palette.get(borderChar, rand);
                        if (b != null) driver.setBlockNoNeighbors(x, yy, z, b);
                    }
                }

                // если под зданием пустота — заполняем filler на lowestLevel
                BlockState at = driver.getBlock(x, lowestLevel, z);
                if (Highways.isEmpty(at)) {
                    driver.setBlockNoNeighbors(x, lowestLevel, z, filler);
                }

                // чистим внутренности (включая границы), чтобы террейн не “торчал” внутри дома
                driver.setBlockRangeToAir(x, lowestLevel + 1, z, topClear);
            }
        }
    }

    /** Отверстия в потолке над лестницами. Как в оригинале: только над клетками с лестницами,
     * от «сразу над верхней ступенью» до пола след. этажа (baseY+6). Не чистим baseY+7 — иначе провал. */
    private void createStairOpenings(ChunkDriver driver, BuildingInfo info, int floorIndex, Transform buildingRotation) {
        int baseY = info.getYForFloor(floorIndex);
        BlockState air = Blocks.AIR.getDefaultState();
        Set<String> stairPositions = new HashSet<>();
        int maxStairSlice = -1;

        for (int slice = 0; slice < 6; slice++) {
            int y = baseY + slice;
            if (y >= driver.world.getTopY()) break;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockState block = driver.getBlock(x, y, z);
                    if (block.getBlock() instanceof net.minecraft.block.StairsBlock) {
                        stairPositions.add(x + "," + z);
                        if (slice > maxStairSlice) maxStairSlice = slice;
                    }
                }
            }
        }

        if (maxStairSlice < 0 || stairPositions.isEmpty()) return;

        // Чистим от (сразу над верхней ступенью) до baseY+6 включительно. baseY+7 не трогаем.
        int clearFromY = baseY + maxStairSlice + 1;
        int clearToY = baseY + 6;

        for (String posStr : stairPositions) {
            String[] parts = posStr.split(",");
            int x = Integer.parseInt(parts[0]);
            int z = Integer.parseInt(parts[1]);
            for (int y = clearFromY; y <= clearToY && y < driver.world.getTopY(); y++) {
                BlockState b = driver.getBlock(x, y, z);
                if (!b.isAir() && b.getBlock() != Blocks.STRUCTURE_VOID) {
                    driver.setBlockNoNeighbors(x, y, z, air);
                }
            }
        }
    }


    // === ШАГ 9: УЛИЦЫ И ДОРОГИ ===

    private void generateStreet(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ) {
        int height = info.getCityGroundLevel();
        CompiledPalette streetPalette = AssetRegistries.getStreetPalette();
        if (streetPalette == null) {
            ModLogger.warn("No street palette (common+default), skipping street for chunk ({}, {})", chunkX, chunkZ);
            return;
        }
        BuildingInfo.StreetType st = info.streetType != null ? info.streetType : BuildingInfo.StreetType.NORMAL;
        switch (st) {
            case NORMAL -> generateNormalStreetSection(info, height, streetPalette, driver);
            case FULL -> generateFullStreetSection(info, height, streetPalette, driver);
            case PARK -> generateParkSection(info, height, streetPalette, driver);
        }
        int frontHeight = height + 1;
        long seed = (driver.world != null ? driver.world.getSeed() : 0) + (long) info.chunkPos.z * 341873128712L + (long) info.chunkPos.x * 132897987541L;
        generateFrontPart(driver, info, info.getXmin(), Transform.ROTATE_NONE, frontHeight, streetPalette, seed);
        generateFrontPart(driver, info, info.getZmin(), Transform.ROTATE_90, frontHeight, streetPalette, seed);
        generateFrontPart(driver, info, info.getXmax(), Transform.ROTATE_180, frontHeight, streetPalette, seed);
        generateFrontPart(driver, info, info.getZmax(), Transform.ROTATE_270, frontHeight, streetPalette, seed);
    }

    /** Фасад (front) со стороны здания adj — на улице у границы с зданием. */
    private void generateFrontPart(ChunkDriver driver, BuildingInfo streetInfo, BuildingInfo adj,
                                    Transform transform, int height, CompiledPalette palette, long seed) {
        if (!streetInfo.hasFrontPartFrom(adj) || adj.frontType == null) return;
        BuildingPart part = AssetRegistries.getPart(adj.frontType);
        if (part == null) return;
        generatePartForStreet(driver, part, transform, height, palette, seed);
    }

    private void generateNormalStreetSection(BuildingInfo info, int height, CompiledPalette palette, ChunkDriver driver) {
        StreetParts parts = StreetParts.DEFAULT;
        boolean xmin = BuildingInfo.hasRoadConnection(info, info.getXmin());
        boolean xmax = BuildingInfo.hasRoadConnection(info, info.getXmax());
        boolean zmin = BuildingInfo.hasRoadConnection(info, info.getZmin());
        boolean zmax = BuildingInfo.hasRoadConnection(info, info.getZmax());
        int cnt = (xmin ? 1 : 0) + (xmax ? 1 : 0) + (zmin ? 1 : 0) + (zmax ? 1 : 0);
        long seed = (driver.world != null ? driver.world.getSeed() : 0) + (long) info.chunkPos.z * 341873128712L + (long) info.chunkPos.x * 132897987541L;
        Transform transform = Transform.ROTATE_NONE;
        BuildingPart part = null;
        List<String> partIds;
        switch (cnt) {
            case 0 -> {
                partIds = parts.none();
                part = AssetRegistries.getPart(getRandomPart(partIds, seed));
            }
            case 1 -> {
                partIds = parts.end();
                part = AssetRegistries.getPart(getRandomPart(partIds, seed));
                if (xmax) transform = Transform.ROTATE_180;
                else if (zmin) transform = Transform.ROTATE_90;
                else if (zmax) transform = Transform.ROTATE_270;
            }
            case 2 -> {
                if (xmin == xmax || zmin == zmax) {
                    partIds = parts.straight();
                    part = AssetRegistries.getPart(getRandomPart(partIds, seed));
                    if (xmax) transform = Transform.ROTATE_180;
                    else if (zmin) transform = Transform.ROTATE_90;
                    else if (zmax) transform = Transform.ROTATE_270;
                } else {
                    partIds = parts.bend();
                    part = AssetRegistries.getPart(getRandomPart(partIds, seed));
                    if (xmin && zmax) transform = Transform.ROTATE_270;
                    else if (xmax && zmin) transform = Transform.ROTATE_90;
                    else if (xmax && zmax) transform = Transform.ROTATE_180;
                }
            }
            case 3 -> {
                partIds = parts.t();
                part = AssetRegistries.getPart(getRandomPart(partIds, seed));
                if (!xmin) transform = Transform.ROTATE_90;
                else if (!xmax) transform = Transform.ROTATE_270;
                else if (!zmin) transform = Transform.ROTATE_180;
            }
            default -> {
                partIds = parts.all();
                part = AssetRegistries.getPart(getRandomPart(partIds, seed));
            }
        }
        if (part != null) {
            generatePartForStreet(driver, part, transform, height, palette, seed);
        }
    }

    private void generateFullStreetSection(BuildingInfo info, int height, CompiledPalette palette, ChunkDriver driver) {
        long seed = (driver.world != null ? driver.world.getSeed() : 0) + (long) info.chunkPos.z * 341873128712L + (long) info.chunkPos.x * 132897987541L;
        String partId = getRandomPart(StreetParts.DEFAULT.full(), seed);
        BuildingPart part = AssetRegistries.getPart(partId);
        if (part != null) {
            generatePartForStreet(driver, part, Transform.ROTATE_NONE, height, palette, seed);
        }
    }

    private void generateParkSection(BuildingInfo info, int height, CompiledPalette palette, ChunkDriver driver) {
        long seed = (driver.world != null ? driver.world.getSeed() : 0) + (long) info.chunkPos.z * 341873128712L + (long) info.chunkPos.x * 132897987541L;
        String partId = getRandomPart(StreetParts.DEFAULT.full(), seed);
        BuildingPart part = AssetRegistries.getPart(partId);
        if (part != null) {
            generatePartForStreet(driver, part, Transform.ROTATE_NONE, height, palette, seed);
        }
    }

    private static String getRandomPart(List<String> parts, long seed) {
        if (parts == null || parts.isEmpty()) return "lostcities:street_full";
        if (parts.size() == 1) return parts.get(0);
        return parts.get(new java.util.Random(seed).nextInt(parts.size()));
    }

    private void generatePartForStreet(ChunkDriver driver, BuildingPart part, Transform transform, int baseY, CompiledPalette palette, long seed) {
        net.minecraft.util.math.random.Random rand = net.minecraft.util.math.random.Random.create(seed);
        for (int slice = 0; slice < part.getSliceCount(); slice++) {
            int y = baseY + slice;
            if (y < driver.world.getBottomY() || y >= driver.world.getTopY()) continue;
            for (int px = 0; px < part.getXSize() && px < 16; px++) {
                for (int pz = 0; pz < part.getZSize() && pz < 16; pz++) {
                    char c = part.getChar(px, slice, pz);
                    if (c == ' ' || c == '\0') continue;
                    BlockState state = palette.get(c, rand);
                    if (state == null) state = Blocks.STONE_BRICKS.getDefaultState();
                    // ВАЖНО: в оригинале ротация ВСЕГДА происходит относительно чанка 16x16, независимо от размера part!
                    int lx = transform.rotateX(px, pz);
                    int lz = transform.rotateZ(px, pz);
                    if (lx < 0 || lx > 15 || lz < 0 || lz > 15) continue;
                    
                    // ВАЖНО: применяем ротацию к BlockState ПЕРЕД correctBlockState (как в оригинале)
                    if (transform != Transform.ROTATE_NONE) {
                        state = transformBlockState(transform, state);
                    }
                    
                    BlockState corrected = correctBlockState(state, lx, y, lz, driver);
                    if (corrected != null) driver.setBlock(lx, y, lz, corrected);
                }
            }
        }
    }
    
    /**
     * Ротация BlockState (оригинал: transformBlockState).
     * Ротирует блоки с тегом rotatable и рельсы.
     */
    private BlockState transformBlockState(Transform transform, BlockState state) {
        if (transform == Transform.ROTATE_NONE) {
            return state;
        }
        
        // Проверяем тег rotatable (как в оригинале: Tools.hasTag(b.getBlock(), LostTags.ROTATABLE_TAG))
        // В Fabric используем TagKey и проверяем через state.isIn()
        var rotatableTag = net.minecraft.registry.tag.TagKey.of(
            net.minecraft.registry.RegistryKeys.BLOCK,
            net.minecraft.util.Identifier.of("lostcities", "rotatable")
        );
        if (state.isIn(rotatableTag)) {
            return state.rotate(transform.getBlockRotation());
        }
        
        // Для рельсов нужна специальная обработка (пока пропускаем, как в оригинале)
        // Для лестниц и других блоков с FACING - ротируем всегда (fallback для блоков без тега)
        if (state.contains(net.minecraft.block.StairsBlock.FACING) || 
            state.contains(net.minecraft.block.DoorBlock.FACING) ||
            state.contains(net.minecraft.block.HorizontalFacingBlock.FACING)) {
            return state.rotate(transform.getBlockRotation());
        }
        
        return state;
    }
    
    /**
     * Корректировать BlockState на основе соседних блоков (для стекол, панелей, стен)
     * Портировано из оригинального ChunkDriver.correct()
     */
    private BlockState correctBlockState(BlockState state, int localX, int y, int localZ, ChunkDriver driver) {
        if (state == null) {
            return null;
        }
        
        // StructureVoid - пропускаем (как альфа-канал)
        if (state.getBlock() instanceof net.minecraft.block.StructureVoidBlock) {
            return null;
        }
        
        // Получаем соседние блоки
        BlockState westState = getNeighborBlock(driver, localX - 1, y, localZ);
        BlockState eastState = getNeighborBlock(driver, localX + 1, y, localZ);
        BlockState northState = getNeighborBlock(driver, localX, y, localZ - 1);
        BlockState southState = getNeighborBlock(driver, localX, y, localZ + 1);
        
        // Обрабатываем HorizontalConnectingBlock (Fabric/Yarn: стеклянные панели, заборы)
        if (state.getBlock() instanceof net.minecraft.block.HorizontalConnectingBlock) {
            boolean w = canAttach(westState);
            boolean e = canAttach(eastState);
            boolean n = canAttach(northState);
            boolean s = canAttach(southState);
            state = state.with(net.minecraft.block.HorizontalConnectingBlock.WEST, w)
                         .with(net.minecraft.block.HorizontalConnectingBlock.EAST, e)
                         .with(net.minecraft.block.HorizontalConnectingBlock.NORTH, n)
                         .with(net.minecraft.block.HorizontalConnectingBlock.SOUTH, s);
            
            // Двусторонняя связь: обновляем ранее поставленных соседей (запад и север)
            updateNeighborPane(driver, localX - 1, y, localZ, net.minecraft.block.HorizontalConnectingBlock.EAST);
            updateNeighborPane(driver, localX, y, localZ - 1, net.minecraft.block.HorizontalConnectingBlock.SOUTH);
        }
        // Обрабатываем WallBlock (стены) — Yarn: *_SHAPE + WallShape
        else if (state.getBlock() instanceof net.minecraft.block.WallBlock) {
            net.minecraft.block.enums.WallShape w = canAttachWall(westState);
            net.minecraft.block.enums.WallShape e = canAttachWall(eastState);
            net.minecraft.block.enums.WallShape n = canAttachWall(northState);
            net.minecraft.block.enums.WallShape s = canAttachWall(southState);
            state = state.with(net.minecraft.block.WallBlock.WEST_SHAPE, w)
                         .with(net.minecraft.block.WallBlock.EAST_SHAPE, e)
                         .with(net.minecraft.block.WallBlock.NORTH_SHAPE, n)
                         .with(net.minecraft.block.WallBlock.SOUTH_SHAPE, s);
            
            // Двусторонняя связь для WallBlock: обновляем ранее поставленных соседей
            updateNeighborWall(driver, localX - 1, y, localZ, net.minecraft.block.WallBlock.EAST_SHAPE, canAttachWall(state));
            updateNeighborWall(driver, localX, y, localZ - 1, net.minecraft.block.WallBlock.SOUTH_SHAPE, canAttachWall(state));
        }
        // Обрабатываем StairBlock — форма по соседям (оригинал: ChunkDriver.getShapeProperty)
        else if (state.getBlock() instanceof net.minecraft.block.StairsBlock) {
            state = state.with(net.minecraft.block.StairsBlock.SHAPE,
                getStairsShape(state, driver, localX, y, localZ));
        }
        
        return state;
    }

    private static boolean isBlockStairs(BlockState state) {
        return state.getBlock() instanceof net.minecraft.block.StairsBlock;
    }

    private static boolean isDifferentStairs(BlockState state, BlockState other) {
        if (!isBlockStairs(other)) return true;
        return other.get(net.minecraft.block.StairsBlock.FACING) != state.get(net.minecraft.block.StairsBlock.FACING)
            || other.get(net.minecraft.block.StairsBlock.HALF) != state.get(net.minecraft.block.StairsBlock.HALF);
    }

    private BlockState getNeighborInDirection(ChunkDriver driver, int lx, int y, int lz, net.minecraft.util.math.Direction dir) {
        return switch (dir) {
            case WEST -> getNeighborBlock(driver, lx - 1, y, lz);
            case EAST -> getNeighborBlock(driver, lx + 1, y, lz);
            case NORTH -> getNeighborBlock(driver, lx, y, lz - 1);
            case SOUTH -> getNeighborBlock(driver, lx, y, lz + 1);
            default -> Blocks.AIR.getDefaultState();
        };
    }

    private net.minecraft.block.enums.StairShape getStairsShape(BlockState state, ChunkDriver driver, int lx, int y, int lz) {
        net.minecraft.util.math.Direction direction = state.get(net.minecraft.block.StairsBlock.FACING);
        BlockState front = getNeighborInDirection(driver, lx, y, lz, direction);
        BlockState back = getNeighborInDirection(driver, lx, y, lz, direction.getOpposite());

        if (isBlockStairs(front) && state.get(net.minecraft.block.StairsBlock.HALF) == front.get(net.minecraft.block.StairsBlock.HALF)) {
            net.minecraft.util.math.Direction frontFacing = front.get(net.minecraft.block.StairsBlock.FACING);
            BlockState frontOpp = getNeighborInDirection(driver, lx, y, lz, frontFacing.getOpposite());
            if (frontFacing.getAxis() != direction.getAxis() && isDifferentStairs(state, frontOpp)) {
                return frontFacing == direction.rotateYCounterclockwise() ? net.minecraft.block.enums.StairShape.OUTER_LEFT : net.minecraft.block.enums.StairShape.OUTER_RIGHT;
            }
        }
        if (isBlockStairs(back) && state.get(net.minecraft.block.StairsBlock.HALF) == back.get(net.minecraft.block.StairsBlock.HALF)) {
            net.minecraft.util.math.Direction backFacing = back.get(net.minecraft.block.StairsBlock.FACING);
            BlockState backSide = getNeighborInDirection(driver, lx, y, lz, backFacing);
            if (backFacing.getAxis() != direction.getAxis() && isDifferentStairs(state, backSide)) {
                return backFacing == direction.rotateYCounterclockwise() ? net.minecraft.block.enums.StairShape.INNER_LEFT : net.minecraft.block.enums.StairShape.INNER_RIGHT;
            }
        }
        return net.minecraft.block.enums.StairShape.STRAIGHT;
    }
    
    /**
     * Получить соседний блок.
     * Важно: НЕ ограничиваемся 0..15, иначе соединяемые блоки (стены/панели) на границе чанка
     * никогда не "увидят" соседа в соседнем чанке и не соединятся.
     */
    private void updateNeighborPane(ChunkDriver driver, int lx, int y, int lz, net.minecraft.state.property.BooleanProperty sideProp) {
        BlockState nb = getNeighborBlock(driver, lx, y, lz);
        if (nb != null && nb.getBlock() instanceof net.minecraft.block.HorizontalConnectingBlock) {
            nb = nb.with(sideProp, true);
            int absoluteX = driver.getChunkX() * 16 + lx;
            int absoluteZ = driver.getChunkZ() * 16 + lz;
            driver.world.setBlockState(new BlockPos(absoluteX, y, absoluteZ), nb, 2);
        }
    }

    private void updateNeighborWall(ChunkDriver driver, int lx, int y, int lz, net.minecraft.state.property.Property<net.minecraft.block.enums.WallShape> shapeProp, net.minecraft.block.enums.WallShape shape) {
        BlockState nb = getNeighborBlock(driver, lx, y, lz);
        if (nb != null && nb.getBlock() instanceof net.minecraft.block.WallBlock) {
            nb = nb.with(shapeProp, shape);
            int absoluteX = driver.getChunkX() * 16 + lx;
            int absoluteZ = driver.getChunkZ() * 16 + lz;
            driver.world.setBlockState(new BlockPos(absoluteX, y, absoluteZ), nb, 2);
        }
    }

    private BlockState getNeighborBlock(ChunkDriver driver, int localX, int y, int localZ) {
        int absoluteX = driver.getChunkX() * 16 + localX;
        int absoluteZ = driver.getChunkZ() * 16 + localZ;
        BlockPos pos = new BlockPos(absoluteX, y, absoluteZ);
        
        return driver.world.getBlockState(pos);
    }
    
    /**
     * Проверить, может ли блок присоединиться к другому блоку
     * Портировано из оригинального ChunkDriver.canAttach()
     */
    private static boolean canAttach(BlockState neighbor) {
        if (neighbor == null || neighbor.isAir()) {
            return false;
        }
        if (neighbor.getBlock() instanceof net.minecraft.block.HorizontalConnectingBlock ||
            neighbor.getBlock() instanceof net.minecraft.block.PaneBlock ||
            neighbor.getBlock() instanceof net.minecraft.block.FenceBlock ||
            neighbor.getBlock() instanceof net.minecraft.block.WallBlock) {
            return true;
        }
        if (neighbor.isOpaque() || neighbor.isFullCube(net.minecraft.world.EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
            return true;
        }
        return !neighbor.isIn(net.minecraft.registry.tag.BlockTags.LEAVES);
    }
    
    /**
     * Проверить, может ли стена присоединиться к блоку (Yarn: WallShape)
     */
    private static net.minecraft.block.enums.WallShape canAttachWall(BlockState state) {
        return canAttach(state) ? net.minecraft.block.enums.WallShape.LOW : net.minecraft.block.enums.WallShape.NONE;
    }
    
    /**
     * Парсинг blockId строки в BlockState с поддержкой свойств
     * Формат: "minecraft:stone_bricks" или "minecraft:oak_door[half=lower,hinge=left,facing=east]"
     */
    private BlockState parseBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) return null;
        
        try {
            // Проверяем, есть ли свойства в квадратных скобках
            if (blockId.contains("[")) {
                String[] parts = blockId.split("\\[", 2);
                String blockIdOnly = parts[0].trim();
                String propertiesStr = parts[1].replace("]", "").trim();
                
                Identifier id = Identifier.tryParse(blockIdOnly);
                if (id == null) {
                    ModLogger.warn("Invalid block ID format: '{}'", blockId);
                    return null;
                }
                
                Block block = Registries.BLOCK.get(id);
                if (block == null) {
                    ModLogger.warn("Block '{}' not found in registry", id);
                    return null;
                }
                
                BlockState state = block.getDefaultState();
                
                // Парсим свойства (формат: "half=lower,hinge=left,facing=east")
                String[] props = propertiesStr.split(",");
                for (String prop : props) {
                    String[] keyValue = prop.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();
                        
                        // Ищем свойство и устанавливаем значение
                        for (net.minecraft.state.property.Property<?> property : state.getProperties()) {
                            if (property.getName().equals(key)) {
                                try {
                                    Collection<?> values = property.getValues();
                                    for (Object propValue : values) {
                                        if (propValue.toString().equalsIgnoreCase(value)) {
                                            state = setPropertyValue(state, property, propValue);
                                            break;
                                        }
                                    }
                                } catch (Exception e) {
                                    // Игнорируем ошибки установки свойства
                                }
                                break;
                            }
                        }
                    }
                }
                
                return state;
            } else {
                // Простой случай - без свойств
                Identifier id = Identifier.tryParse(blockId.trim());
                if (id == null) {
                    ModLogger.warn("Invalid block ID format: '{}'", blockId);
                    return null;
                }
                
                Block block = Registries.BLOCK.get(id);
                if (block != null) {
                    return block.getDefaultState();
                } else {
                    ModLogger.warn("Block '{}' not found in registry", id);
                }
            }
        } catch (Exception e) {
            ModLogger.warn("Failed to parse block '{}': {}", blockId, e.getMessage());
        }
        
        return null;
    }
    
    /** Хелпер: установить enum-свойство без жёсткой типизации. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private BlockState setPropertyValue(BlockState state, net.minecraft.state.property.Property prop, Object value) {
        return state.with(prop, (Comparable) value);
    }
    
    /**
     * Получить количество вызовов для статистики
     */
    public static int getCallCount() {
        return callCount;
    }
    
    /**
     * Сбросить счётчик (для тестирования)
     */
    public static void resetCallCount() {
        callCount = 0;
        ModLogger.info("LostCityFeature call counter reset");
    }
    
    /**
     * Проверить, является ли колонка void (пустой от верха до низа) в указанной точке.
     * Оригинал: LostCityTerrainFeature.isVoid(int x, int z).
     * 
     * @param driver ChunkDriver для доступа к блокам
     * @param x Локальная координата X (0-15)
     * @param z Локальная координата Z (0-15)
     * @return true если колонка void (пустая от верха до низа)
     */
    private static boolean isVoidAt(ChunkDriver driver, int x, int z) {
        int maxY = driver.world.getTopY() - 1;
        int minY = driver.world.getBottomY();
        
        // Проверяем колонку сверху вниз
        for (int y = maxY; y >= minY; y--) {
            net.minecraft.block.BlockState state = driver.getBlock(x, y, z);
            if (!state.isAir() && !state.getBlock().equals(net.minecraft.block.Blocks.STRUCTURE_VOID)) {
                // Нашли не-воздушный блок - колонка не void
                return false;
            }
        }
        // Вся колонка пустая - void
        return true;
    }
    
    // === TERRAIN CORRECTION INTEGRATION (Этап 2.1) ===
    
    /**
     * Корректировать форму террейна на основе desired heights из соседних городских чанков.
     * Оригинал: LostCityTerrainFeature.correctTerrainShape().
     * 
     * @param driver ChunkDriver для доступа к блокам
     * @param info BuildingInfo текущего чанка
     * @param chunkX Координата X чанка
     * @param chunkZ Координата Z чанка
     * @param heightmapHeight Текущая высота heightmap
     */
    private void correctTerrainShape(ChunkDriver driver, BuildingInfo info, int chunkX, int chunkZ, int heightmapHeight) {
        // Получаем MinMax для всех 4 углов чанка
        BuildingInfo.MinMax mm00 = info.getDesiredMaxHeightL2();
        BuildingInfo.MinMax mm10 = info.getXmax().getDesiredMaxHeightL2();
        BuildingInfo.MinMax mm01 = info.getZmax().getDesiredMaxHeightL2();
        BuildingInfo.MinMax mm11 = info.getXmax().getZmax().getDesiredMaxHeightL2();
        
        int min = driver.world.getBottomY();
        int max = driver.world.getTopY() - 1;
        
        float min00 = mm00.min;
        float min10 = mm10.min;
        float min01 = mm01.min;
        float min11 = mm11.min;
        float max00 = mm00.max;
        float max10 = mm10.max;
        float max01 = mm01.max;
        float max11 = mm11.max;
        
        // Проверяем, нужно ли корректировать террейн
        if (max00 < max || max10 < max || max01 < max || max11 < max ||
                min00 < max || min10 < max || min01 < max || min11 < max) {
            // Нужно скорректировать террейн между верхней и нижней сеткой
            int maxHeightP = heightmapHeight + 90;
            int minHeightP = heightmapHeight - 90;
            
            // Корректируем значения, которые выходят за пределы
            if (max00 >= max) max00 = maxHeightP;
            if (max10 >= max) max10 = maxHeightP;
            if (max01 >= max) max01 = maxHeightP;
            if (max11 >= max) max11 = maxHeightP;
            if (min00 >= max) min00 = minHeightP;
            if (min10 >= max) min10 = minHeightP;
            if (min01 >= max) min01 = minHeightP;
            if (min11 >= max) min11 = minHeightP;
            
            // Билинейная интерполяция для каждой точки в чанке
            for (int x = 0; x < 16; x++) {
                float factorX = (15.0f - x) / 15.0f;
                float maxh0 = max11 + (max01 - max11) * factorX;
                float maxh1 = max10 + (max00 - max10) * factorX;
                float minh0 = min11 + (min01 - min11) * factorX;
                float minh1 = min10 + (min00 - min10) * factorX;
                
                for (int z = 0; z < 16; z++) {
                    float factorZ = (15.0f - z) / 15.0f;
                    float maxheight = maxh0 + (maxh1 - maxh0) * factorZ;
                    if (maxheight > max) {
                        maxheight = max;
                    }
                    
                    // Перемещаем блоки вниз, если нужно
                    int maxTouchedY = moveDown(driver, x, z, (int) maxheight, max);
                    
                    // Если ничего не было перемещено вниз, перемещаем вверх
                    if (maxTouchedY == Short.MIN_VALUE) {
                        float minheight = minh0 + (minh1 - minh0) * factorZ;
                        if (minheight < min) {
                            minheight = min;
                        }
                        maxTouchedY = moveUp(driver, x, z, (int) minheight, info.getWaterLevel() > info.groundLevel);
                    }
                }
            }
        }
    }
    
    /**
     * Переместить блоки вниз в колонке. Оригинал: LostCityTerrainFeature.moveDown().
     * 
     * @param driver ChunkDriver для доступа к блокам
     * @param x Локальная координата X (0-15)
     * @param z Локальная координата Z (0-15)
     * @param height Целевая высота
     * @param maxBuildLimit Максимальная высота постройки
     * @return Максимальная Y координата, которую мы затронули, или Short.MIN_VALUE если ничего не сделано
     */
    private int moveDown(ChunkDriver driver, int x, int z, int height, int maxBuildLimit) {
        int maxYTouched = Short.MIN_VALUE;
        int y = maxBuildLimit - 1;
        
        // Находим первый не-воздушный блок сверху вниз
        while (y > height) {
            net.minecraft.block.BlockState state = driver.getBlock(x, y, z);
            if (!isEmpty(state)) {
                break;
            }
            y--;
        }
        
        if (y <= height) {
            return maxYTouched; // Ничего не нужно делать
        }
        
        // Буфер для блоков, которые нужно переместить
        net.minecraft.block.BlockState[] buffer = new net.minecraft.block.BlockState[6];
        int bufferIdx = 0;
        
        // Сохраняем блоки выше height в буфер
        int currentY = y;
        while (currentY >= height && bufferIdx < buffer.length) {
            buffer[bufferIdx++] = driver.getBlock(x, currentY, z);
            driver.setBlockNoNeighbors(x, currentY, z, net.minecraft.block.Blocks.AIR.getDefaultState());
            currentY--;
        }
        
        maxYTouched = currentY;
        
        // Перемещаем блоки из буфера вниз
        int idx = 0;
        while (idx < bufferIdx && currentY > driver.world.getBottomY()) {
            driver.setBlockNoNeighbors(x, currentY, z, buffer[idx++]);
            currentY--;
        }
        
        return maxYTouched;
    }
    
    /**
     * Переместить блоки вверх в колонке. Оригинал: LostCityTerrainFeature.moveUp().
     * 
     * @param driver ChunkDriver для доступа к блокам
     * @param x Локальная координата X (0-15)
     * @param z Локальная координата Z (0-15)
     * @param height Целевая высота
     * @param dowater Нужно ли учитывать воду
     * @return Максимальная Y координата, которую мы затронули, или Short.MIN_VALUE если ничего не сделано
     */
    private int moveUp(ChunkDriver driver, int x, int z, int height, boolean dowater) {
        int maxYTouched = Short.MIN_VALUE;
        int minHeight = driver.world.getBottomY();
        
        // Находим первый не-пустой блок снизу вверх, начиная с height
        int y = height;
        while (y > minHeight) {
            net.minecraft.block.BlockState state = driver.getBlock(x, y, z);
            if (!isFoliageOrEmpty(state)) {
                break;
            }
            y--;
        }
        
        if (y >= height) {
            return maxYTouched; // Ничего не нужно делать
        }
        
        // Перемещаем блоки вверх
        int idx = y; // Указывает на не-пустой блок ниже пустого блока
        int targetY = height;
        
        while (idx > minHeight && targetY > minHeight) {
            net.minecraft.block.BlockState blockToMove = driver.getBlock(x, idx, z);
            if (blockToMove.isAir() || blockToMove.getBlock() == net.minecraft.block.Blocks.BEDROCK) {
                break;
            }
            if (maxYTouched == Short.MIN_VALUE) {
                maxYTouched = idx;
            }
            driver.setBlockNoNeighbors(x, targetY, z, blockToMove);
            driver.setBlockNoNeighbors(x, idx, z, net.minecraft.block.Blocks.AIR.getDefaultState());
            targetY--;
            idx--;
        }
        
        return maxYTouched;
    }
    
    /**
     * Проверить, является ли блок листвой или пустым. Оригинал: LostCityTerrainFeature.isFoliageOrEmpty().
     * 
     * @param state BlockState для проверки
     * @return true если блок листва или пустой
     */
    private static boolean isFoliageOrEmpty(net.minecraft.block.BlockState state) {
        if (isEmpty(state)) {
            return true;
        }
        // Проверяем, является ли блок листвой (листья, трава, и т.д.)
        // В Fabric/Yarn используем теги блоков
        return state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES);
    }
    
    /**
     * Проверить, является ли блок пустым (воздух, вода, лава). Оригинал: LostCityTerrainFeature.isEmpty().
     * 
     * @param state BlockState для проверки
     * @return true если блок пустой
     */
    private static boolean isEmpty(net.minecraft.block.BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.getBlock() == net.minecraft.block.Blocks.WATER) {
            return true;
        }
        if (state.getBlock() == net.minecraft.block.Blocks.LAVA) {
            return true;
        }
        if (state.getBlock() == net.minecraft.block.Blocks.STRUCTURE_VOID) {
            return true;
        }
        return false;
    }
}
