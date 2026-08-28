package com.lostcity.worldgen;

import com.lostcity.util.ModLogger;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;

/**
 * Драйвер для эффективного размещения блоков в чанке
 * Портирован из ChunkDriver (оригинальный Forge мод)
 * 
 * УПРОЩЁННАЯ ВЕРСИЯ для Шага 4:
 * - Прямое размещение блоков без кэширования
 * - Базовая работа с heightmap
 * - Поддержка координат внутри чанка (0-15)
 */
public class ChunkDriver {
    
    public final StructureWorldAccess world;
    private final Chunk chunk;
    private final int chunkX;
    private final int chunkZ;
    private final int baseX; // Абсолютная X координата чанка (chunkX * 16)
    private final int baseZ; // Абсолютная Z координата чанка (chunkZ * 16)
    
    // Статистика
    private int blocksPlaced = 0;
    
    /**
     * Создать драйвер для работы с чанком
     */
    public ChunkDriver(StructureWorldAccess world, Chunk chunk) {
        this.world = world;
        this.chunk = chunk;
        this.chunkX = chunk.getPos().x;
        this.chunkZ = chunk.getPos().z;
        this.baseX = chunkX * 16;
        this.baseZ = chunkZ * 16;
    }
    
    /**
     * Установить блок по ЛОКАЛЬНЫМ координатам внутри чанка (0-15)
     * 
     * @param localX Локальная X (0-15)
     * @param y Абсолютная Y
     * @param localZ Локальная Z (0-15)
     * @param state BlockState для установки
     */
    public void setBlock(int localX, int y, int localZ, BlockState state) {
        // Проверка границ
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            ModLogger.warn("setBlock: coordinates out of chunk bounds: ({}, {}, {})", 
                localX, y, localZ);
            return;
        }
        
        if (y < world.getBottomY() || y >= world.getTopY()) {
            // Выходит за границы мира - пропускаем
            return;
        }
        
        // Преобразуем в абсолютные координаты
        int absoluteX = baseX + localX;
        int absoluteZ = baseZ + localZ;
        BlockPos pos = new BlockPos(absoluteX, y, absoluteZ);
        
        world.setBlockState(pos, state, 2);

        // Обновляем соседей только для блоков с соединениями (стекло, стены, заборы)
        if (connectsToNeighbors(state)) {
            updateNeighbors(pos, state);
        }

        blocksPlaced++;
    }

    /** Нужно ли обновлять соседей (соединяемые блоки). Остальные — setBlockNoNeighbors по сути. */
    private static boolean connectsToNeighbors(BlockState state) {
        return state.getBlock() instanceof net.minecraft.block.HorizontalConnectingBlock
            || state.getBlock() instanceof net.minecraft.block.WallBlock
            || state.getBlock() instanceof net.minecraft.block.PaneBlock
            || state.getBlock() instanceof net.minecraft.block.FenceBlock;
    }

    /**
     * Установить блок без обновления соседей (для массовой заливки filler/air, проёмы).
     * Быстрее при генерации дверей/проёмов.
     */
    public void setBlockNoNeighbors(int localX, int y, int localZ, BlockState state) {
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) return;
        if (y < world.getBottomY() || y >= world.getTopY()) return;
        int absoluteX = baseX + localX;
        int absoluteZ = baseZ + localZ;
        BlockPos pos = new BlockPos(absoluteX, y, absoluteZ);
        world.setBlockState(pos, state, 2);
        blocksPlaced++;
    }

    /**
     * Заполнить вертикальную колонку (x,z) от fromY до toY включительно.
     * Как в оригинале Doors.setBlockRange. Без обновления соседей.
     */
    public void setBlockRange(int localX, int fromY, int localZ, int toY, BlockState state) {
        int minY = Math.min(fromY, toY);
        int maxY = Math.max(fromY, toY);
        for (int y = minY; y <= maxY; y++) {
            setBlockNoNeighbors(localX, y, localZ, state);
        }
    }
    
    /**
     * Обновить соседние блоки для правильного соединения стекол, панелей и т.д.
     * Портировано из оригинального ChunkDriver.updateAdjacent()
     */
    private void updateNeighbors(BlockPos pos, BlockState placedState) {
        // Обновляем все 6 соседних позиций
        BlockPos[] neighbors = {
            pos.north(), pos.south(), pos.east(), pos.west(), pos.up(), pos.down()
        };
        
        net.minecraft.util.math.Direction[] directions = {
            net.minecraft.util.math.Direction.NORTH,
            net.minecraft.util.math.Direction.SOUTH,
            net.minecraft.util.math.Direction.EAST,
            net.minecraft.util.math.Direction.WEST,
            net.minecraft.util.math.Direction.UP,
            net.minecraft.util.math.Direction.DOWN
        };
        
        for (int i = 0; i < neighbors.length; i++) {
            BlockPos neighborPos = neighbors[i];
            net.minecraft.util.math.Direction direction = directions[i];
            
            try {
                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.isAir()) {
                    continue;
                }
                
                // Обновляем форму соседнего блока на основе размещённого.
                // Вызываем через BlockState, а не через Block: в 1.20.5 метод на
                // AbstractBlock стал protected, а публичный делегат на состоянии
                // существует во всех поддерживаемых версиях.
                BlockState updatedNeighbor = neighborState.getStateForNeighborUpdate(
                    direction,
                    placedState,
                    world,
                    neighborPos,
                    pos
                );
                
                if (updatedNeighbor != neighborState) {
                    // Обновляем соседний блок, если его состояние изменилось
                    world.setBlockState(neighborPos, updatedNeighbor, 2);
                }
            } catch (Exception e) {
                // Игнорируем ошибки при обновлении соседей (может быть проблема с границами чанков)
            }
        }
    }
    
    /** Заполнить слой Y. Террейн не соединяется — без updateNeighbors. */
    public void fillLayer(int y, BlockState state) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                setBlockNoNeighbors(x, y, z, state);
            }
        }
    }

    /** Заполнить колонку. Без updateNeighbors. */
    public void fillColumn(int localX, int localZ, int fromY, int toY, BlockState state) {
        int minY = Math.min(fromY, toY);
        int maxY = Math.max(fromY, toY);
        for (int y = minY; y <= maxY; y++) {
            setBlockNoNeighbors(localX, y, localZ, state);
        }
    }

    /** Очистить колонку (воздух). Оригинал: setBlockRangeToAir. */
    public void setBlockRangeToAir(int localX, int fromY, int localZ, int toY) {
        setBlockRangeToAir(localX, fromY, localZ, toY, s -> true);
    }

    /** Очистить колонку только где predicate.test(block). */
    public void setBlockRangeToAir(int localX, int fromY, int localZ, int toY, java.util.function.Predicate<BlockState> filter) {
        int minY = Math.min(fromY, toY);
        int maxY = Math.max(fromY, toY);
        BlockState air = net.minecraft.block.Blocks.AIR.getDefaultState();
        for (int y = minY; y <= maxY; y++) {
            BlockState cur = getBlock(localX, y, localZ);
            if (filter.test(cur)) setBlockNoNeighbors(localX, y, localZ, air);
        }
    }
    
    /**
     * Получить текущую высоту terrain в точке (для оригинального террейна)
     */
    public int getHeight(int localX, int localZ) {
        int absoluteX = baseX + localX;
        int absoluteZ = baseZ + localZ;
        
        // Получаем высоту из heightmap
        return world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, 
            absoluteX, absoluteZ);
    }
    
    public BlockState getBlock(int localX, int y, int localZ) {
        int absoluteX = baseX + localX;
        int absoluteZ = baseZ + localZ;
        return world.getBlockState(new BlockPos(absoluteX, y, absoluteZ));
    }

    /** Абсолютная позиция по локальным координатам. */
    public BlockPos getBlockPos(int localX, int y, int localZ) {
        return new BlockPos(baseX + localX, y, baseZ + localZ);
    }
    
    /**
     * Получить количество размещённых блоков
     */
    public int getBlocksPlaced() {
        return blocksPlaced;
    }
    
    /**
     * Получить координаты чанка
     */
    public int getChunkX() {
        return chunkX;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Вывести статистику
     */
    public void logStats() {
        if (blocksPlaced > 0) {
            ModLogger.debug("ChunkDriver stats for chunk ({}, {}): {} blocks placed",
                chunkX, chunkZ, blocksPlaced);
        }
    }
}
