package com.lostcity.worldgen;

import com.lostcity.assets.Building;
import com.lostcity.assets.BuildingPart;
import com.lostcity.assets.CompiledPalette;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.Direction;

/**
 * Генерация дверей в зданиях.
 * Портировано из mcjty.lostcities.worldgen.gen.Doors (оригинальный Forge мод).
 * 
 * Этап 1.1: Критическое исправление - добавление генерации дверных блоков.
 */
public class Doors {
    
    private static final String META_DONTCONNECT = "dontconnect";
    
    /**
     * Создать BlockState для двери с правильными свойствами.
     * Оригинал: getDoor() в Doors.java
     */
    private static BlockState getDoor(Block door, boolean upper, boolean left, Direction facing) {
        return door.getDefaultState()
                .with(DoorBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER)
                .with(DoorBlock.FACING, facing)
                .with(DoorBlock.HINGE, left ? net.minecraft.block.enums.DoorHinge.LEFT : net.minecraft.block.enums.DoorHinge.RIGHT);
    }
    
    /**
     * Хелпер: установить enum-свойство без жёсткой типизации (для рефлексии).
     * Использует рефлексию для вызова метода with() напрямую, обходя ограничения типизации.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setPropertyValueUnsafe(BlockState state, net.minecraft.state.property.Property prop, Object value) {
        try {
            // Используем рефлексию для вызова метода with() напрямую
            // Это обходит ограничения generic-типизации компилятора
            java.lang.reflect.Method withMethod = BlockState.class.getMethod("with", 
                net.minecraft.state.property.Property.class, Comparable.class);
            Comparable comparableValue = (Comparable) value;
            return (BlockState) withMethod.invoke(state, prop, comparableValue);
        } catch (Exception e) {
            // Если рефлексия не сработала, возвращаем state без изменений
            // Двери будут работать, но без правильной петли
            return state;
        }
    }

    /**
     * Генерировать двери для здания на указанном этаже.
     * Оригинал: generateDoors() в Doors.java
     * 
     * @param driver ChunkDriver для размещения блоков
     * @param info BuildingInfo здания
     * @param height Высота этажа (baseY + 1, так как двери начинаются с height-1 для filler)
     * @param floorIndex Локальный индекс этажа (от -cellars до floors)
     * @param compiledPalette CompiledPalette для получения filler
     */
    public static void generateDoors(ChunkDriver driver, BuildingInfo info, int height, int floorIndex, CompiledPalette compiledPalette) {
        if (!info.hasBuilding || info.doorBlock == null) {
            return;
        }
        
        BlockState air = Blocks.AIR.getDefaultState();
        Building building = com.lostcity.assets.AssetRegistries.getBuilding(info.buildingType);
        if (building == null) {
            return;
        }
        
        // Получаем filler из CompiledPalette
        char fillerChar = building.getFiller();
        BlockState filler = compiledPalette.get(fillerChar);
        if (filler == null) {
            filler = Blocks.STONE_BRICKS.getDefaultState();
        }
        height--; // Start generating doors one below for the filler
        int level = floorIndex + info.cellars;

        // x=0 (запад): связь с xmin
        if (info.hasConnectionAtX(level)) {
            int x = 0;
            BuildingInfo xmin = info.getXmin();
            if (hasConnectionWithBuilding(floorIndex, info, xmin)) {
                // Связь с зданием - только проём (air), без двери
                driver.setBlockRange(x, height, 6, height + 4, filler);
                driver.setBlockRange(x, height, 9, height + 4, filler);
                driver.setBlockNoNeighbors(x, height, 7, filler);
                driver.setBlockNoNeighbors(x, height + 1, 7, air);
                driver.setBlockNoNeighbors(x, height + 2, 7, air);
                driver.setBlockNoNeighbors(x, height + 3, 7, filler);
                driver.setBlockNoNeighbors(x, height, 8, filler);
                driver.setBlockNoNeighbors(x, height + 1, 8, air);
                driver.setBlockNoNeighbors(x, height + 2, 8, air);
                driver.setBlockNoNeighbors(x, height + 3, 8, filler);
            } else if (hasConnectionToTopOrOutside(floorIndex, info, xmin)) {
                // Связь с улицей/крышей - ставим дверь
                driver.setBlockRange(x, height, 6, height + 4, filler);
                driver.setBlockRange(x, height, 9, height + 4, filler);
                driver.setBlockNoNeighbors(x, height, 7, filler);
                driver.setBlockNoNeighbors(x, height + 1, 7, getDoor(info.doorBlock, false, true, Direction.EAST));
                driver.setBlockNoNeighbors(x, height + 2, 7, getDoor(info.doorBlock, true, true, Direction.EAST));
                driver.setBlockNoNeighbors(x, height + 3, 7, filler);
                driver.setBlockNoNeighbors(x, height, 8, filler);
                driver.setBlockNoNeighbors(x, height + 1, 8, getDoor(info.doorBlock, false, false, Direction.EAST));
                driver.setBlockNoNeighbors(x, height + 2, 8, getDoor(info.doorBlock, true, false, Direction.EAST));
                driver.setBlockNoNeighbors(x, height + 3, 8, filler);
            }
        }
        
        // x=15 (восток): сосед xmax имеет связь с нами
        BuildingInfo xmax = info.getXmax();
        if (hasConnectionWithBuildingMax(floorIndex, info, xmax, BuildingInfo.Orientation.X)) {
            int x = 15;
            driver.setBlockRange(x, height, 6, height + 4, filler);
            driver.setBlockRange(x, height, 9, height + 4, filler);
            driver.setBlockNoNeighbors(x, height, 7, filler);
            driver.setBlockNoNeighbors(x, height + 1, 7, air);
            driver.setBlockNoNeighbors(x, height + 2, 7, air);
            driver.setBlockNoNeighbors(x, height + 3, 7, filler);
            driver.setBlockNoNeighbors(x, height, 8, filler);
            driver.setBlockNoNeighbors(x, height + 1, 8, air);
            driver.setBlockNoNeighbors(x, height + 2, 8, air);
            driver.setBlockNoNeighbors(x, height + 3, 8, filler);
        } else if (hasConnectionToTopOrOutside(floorIndex, info, xmax) && 
                   xmax != null && xmax.hasConnectionAtXFromStreet(floorIndex + xmax.cellars)) {
            // Дверь на восток: как в оригинале — level = f + info.getXmax().cellars (соседские cellars)
            int x = 15;
            driver.setBlockRange(x, height, 6, height + 4, filler);
            driver.setBlockRange(x, height, 9, height + 4, filler);
            driver.setBlockNoNeighbors(x, height, 7, filler);
            driver.setBlockNoNeighbors(x, height + 1, 7, getDoor(info.doorBlock, false, false, Direction.WEST));
            driver.setBlockNoNeighbors(x, height + 2, 7, getDoor(info.doorBlock, true, false, Direction.WEST));
            driver.setBlockNoNeighbors(x, height + 3, 7, filler);
            driver.setBlockNoNeighbors(x, height, 8, filler);
            driver.setBlockNoNeighbors(x, height + 1, 8, getDoor(info.doorBlock, false, true, Direction.WEST));
            driver.setBlockNoNeighbors(x, height + 2, 8, getDoor(info.doorBlock, true, true, Direction.WEST));
            driver.setBlockNoNeighbors(x, height + 3, 8, filler);
        }
        
        // z=0 (север)
        if (info.hasConnectionAtZ(level)) {
            int z = 0;
            BuildingInfo zmin = info.getZmin();
            if (hasConnectionWithBuilding(floorIndex, info, zmin)) {
                // Связь с зданием - только проём
                driver.setBlockRange(6, height, z, height + 4, filler);
                driver.setBlockRange(9, height, z, height + 4, filler);
                driver.setBlockNoNeighbors(7, height, z, filler);
                driver.setBlockNoNeighbors(7, height + 1, z, air);
                driver.setBlockNoNeighbors(7, height + 2, z, air);
                driver.setBlockNoNeighbors(7, height + 3, z, filler);
                driver.setBlockNoNeighbors(8, height, z, filler);
                driver.setBlockNoNeighbors(8, height + 1, z, air);
                driver.setBlockNoNeighbors(8, height + 2, z, air);
                driver.setBlockNoNeighbors(8, height + 3, z, filler);
            } else if (hasConnectionToTopOrOutside(floorIndex, info, zmin)) {
                // Связь с улицей/крышей - ставим дверь
                driver.setBlockRange(6, height, z, height + 4, filler);
                driver.setBlockRange(9, height, z, height + 4, filler);
                driver.setBlockNoNeighbors(7, height, z, filler);
                driver.setBlockNoNeighbors(7, height + 1, z, getDoor(info.doorBlock, false, true, Direction.NORTH));
                driver.setBlockNoNeighbors(7, height + 2, z, getDoor(info.doorBlock, true, true, Direction.NORTH));
                driver.setBlockNoNeighbors(7, height + 3, z, filler);
                driver.setBlockNoNeighbors(8, height, z, filler);
                driver.setBlockNoNeighbors(8, height + 1, z, getDoor(info.doorBlock, false, false, Direction.NORTH));
                driver.setBlockNoNeighbors(8, height + 2, z, getDoor(info.doorBlock, true, false, Direction.NORTH));
                driver.setBlockNoNeighbors(8, height + 3, z, filler);
            }
        }
        
        // z=15 (юг)
        BuildingInfo zmax = info.getZmax();
        if (hasConnectionWithBuildingMax(floorIndex, info, zmax, BuildingInfo.Orientation.Z)) {
            int z = 15;
            driver.setBlockRange(6, height, z, height + 4, filler);
            driver.setBlockRange(9, height, z, height + 4, filler);
            driver.setBlockNoNeighbors(7, height, z, filler);
            driver.setBlockNoNeighbors(7, height + 1, z, air);
            driver.setBlockNoNeighbors(7, height + 2, z, air);
            driver.setBlockNoNeighbors(7, height + 3, z, filler);
            driver.setBlockNoNeighbors(8, height, z, filler);
            driver.setBlockNoNeighbors(8, height + 1, z, air);
            driver.setBlockNoNeighbors(8, height + 2, z, air);
            driver.setBlockNoNeighbors(8, height + 3, z, filler);
        } else if (hasConnectionToTopOrOutside(floorIndex, info, zmax) && 
                   zmax != null && zmax.hasConnectionAtZFromStreet(floorIndex + zmax.cellars)) {
            // Дверь на юг: как в оригинале — level = f + info.getZmax().cellars (соседские cellars)
            int z = 15;
            driver.setBlockRange(6, height, z, height + 4, filler);
            driver.setBlockRange(9, height, z, height + 4, filler);
            driver.setBlockNoNeighbors(7, height, z, filler);
            driver.setBlockNoNeighbors(7, height + 1, z, getDoor(info.doorBlock, false, false, Direction.SOUTH));
            driver.setBlockNoNeighbors(7, height + 2, z, getDoor(info.doorBlock, true, false, Direction.SOUTH));
            driver.setBlockNoNeighbors(7, height + 3, z, filler);
            driver.setBlockNoNeighbors(8, height, z, filler);
            driver.setBlockNoNeighbors(8, height + 1, z, getDoor(info.doorBlock, false, true, Direction.SOUTH));
            driver.setBlockNoNeighbors(8, height + 2, z, getDoor(info.doorBlock, true, true, Direction.SOUTH));
            driver.setBlockNoNeighbors(8, height + 3, z, filler);
        }
    }

    /**
     * Проверка связи с зданием (оригинал: hasConnectionWithBuildingMax).
     */
    private static boolean hasConnectionWithBuildingMax(int localLevel, BuildingInfo info, BuildingInfo adj, BuildingInfo.Orientation orientation) {
        if (adj == null || !adj.hasBuilding) {
            return false;
        }
        
        // Проверка META_DONTCONNECT для текущего здания
        if (info.isValidFloor(localLevel)) {
            BuildingPart floor = info.getFloor(localLevel);
            if (floor != null && floor.getMetaBoolean(META_DONTCONNECT)) {
                return false;
            }
        }
        
        int globalLevel = info.localToGlobal(localLevel);
        int localAdjacent = adj.globalToLocal(globalLevel);
        
        // Проверка META_DONTCONNECT для соседнего здания
        if (adj.isValidFloor(localAdjacent)) {
            BuildingPart adjFloor = adj.getFloor(localAdjacent);
            if (adjFloor != null && adjFloor.getMetaBoolean(META_DONTCONNECT)) {
                return false;
            }
        }
        
        int level = localAdjacent + adj.cellars;
        // Проверяем что сосед имеет связь с нами на том же глобальном уровне
        return ((localAdjacent >= 0 && localAdjacent < adj.getNumFloors()) || 
                (localAdjacent < 0 && (-localAdjacent) <= adj.cellars)) && 
               adj.hasConnectionAt(level, orientation);
    }

    /**
     * Проверка связи с улицей/крышей (оригинал: hasConnectionToTopOrOutside).
     */
    private static boolean hasConnectionToTopOrOutside(int localLevel, BuildingInfo info, BuildingInfo adj) {
        if (adj == null) {
            return false;
        }
        
        // Проверка META_DONTCONNECT
        if (info.isValidFloor(localLevel)) {
            BuildingPart floor = info.getFloor(localLevel);
            if (floor != null && floor.getMetaBoolean(META_DONTCONNECT)) {
                return false;
            }
        }
        
        int globalLevel = info.localToGlobal(localLevel);
        int localAdjacent = adj.globalToLocal(globalLevel);
        
        // Выход на улицу: localLevel == 0 && localAdjacent == 0 && adj.isCity && !adj.hasBuilding
        if (localLevel == 0 && localAdjacent == 0 && adj.isCity && !adj.hasBuilding) {
            return true;
        }
        
        // Выход на крышу соседа: localAdjacent == adj.getNumFloors()
        if (adj.hasBuilding && localAdjacent == adj.getNumFloors()) {
            return true;
        }
        
        return false;
    }

    /**
     * Проверка связи с зданием (оригинал: hasConnectionWithBuilding).
     */
    private static boolean hasConnectionWithBuilding(int localLevel, BuildingInfo info, BuildingInfo adj) {
        if (adj == null || !adj.hasBuilding) {
            return false;
        }
        
        int globalLevel = info.localToGlobal(localLevel);
        int localAdjacent = adj.globalToLocal(globalLevel);
        
        // Проверяем что сосед имеет здание на том же глобальном уровне
        return (localAdjacent >= 0 && localAdjacent < adj.getNumFloors()) || 
               (localAdjacent < 0 && (-localAdjacent) <= adj.cellars);
    }
}
