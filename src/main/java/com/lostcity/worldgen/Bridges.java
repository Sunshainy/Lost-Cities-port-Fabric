package com.lostcity.worldgen;

import com.lostcity.assets.AssetRegistries;
import com.lostcity.assets.BuildingPart;
import com.lostcity.assets.CompiledPalette;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * Генерация мостиков между чанками (bridges). Оригинал: mcjty.lostcities.worldgen.gen.Bridges.
 * Мосты соединяют города через негородские чанки (долины, реки).
 */
public final class Bridges {

    private static final String META_SUPPORT = "support";

    /**
     * Генерирует мост в чанке, если есть. Оригинал: Bridges.generateBridges.
     */
    public static void generateBridges(ChunkDriver driver, BuildingInfo info) {
        // Если highway на уровне 0 — мосты не генерируются
        if (info.highwayXLevel == 0 || info.highwayZLevel == 0) return;

        BuildingPart bridgePart = info.hasXBridge();
        if (bridgePart != null) {
            generateBridge(driver, info, bridgePart, true); // X-orientation
        } else {
            bridgePart = info.hasZBridge();
            if (bridgePart != null) {
                generateBridge(driver, info, bridgePart, false); // Z-orientation
            }
        }
    }

    private static void generateBridge(ChunkDriver driver, BuildingInfo info, BuildingPart part, boolean xOrientation) {
        CompiledPalette palette = AssetRegistries.getStreetPalette();
        if (palette == null || part == null) return;

        // Оригинал: info.profile.GROUNDLEVEL + 1 — фиксированный уровень профиля. Все мосты на одной высоте.
        int profileGround = info.config.getActiveProfile().getGroundLevel();
        int startY = profileGround + 1;
        int sliceCount = part.getSliceCount();

        // Генерация части моста (по аналогии с highway)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int slice = 0; slice < sliceCount; slice++) {
                    int y = startY + slice;
                    if (y >= Heights.topY(driver.world)) break;

                    char c = xOrientation ? part.getChar(x, slice, z) : part.getChar(z, slice, x);
                    if (c == ' ' || c == '\0') continue;

                    BlockState state = palette.get(c, driver.world.getRandom());
                    if (state == null) state = Blocks.STONE_BRICKS.getDefaultState();
                    driver.setBlockNoNeighbors(x, y, z, state);
                }
            }
        }

        // Опоры (support) — если включены
        if (!info.config.getActiveProfile().getBridgeSupports()) return;
        Character supportChar = part.getMetaChar(META_SUPPORT);
        if (supportChar == null) return;
        BlockState support = palette.get(supportChar.charValue(), driver.world.getRandom());
        if (support == null) return;

        // Этап 3.3: Улучшенная генерация опор для мостов (как в оригинале)
        // Опоры в центре (7,7), (7,8), (8,7), (8,8) — если мост проходит в обе стороны
        BuildingInfo minDir = xOrientation ? info.getXmin() : info.getZmin();
        BuildingInfo maxDir = xOrientation ? info.getXmax() : info.getZmax();
        
        BuildingPart minBridge = xOrientation ? minDir.hasXBridge() : minDir.hasZBridge();
        BuildingPart maxBridge = xOrientation ? maxDir.hasXBridge() : maxDir.hasZBridge();
        
        // Опоры в центре, если мост проходит в обе стороны
        if (minBridge != null && maxBridge != null) {
            int waterLevel = info.getWaterLevel();
            for (int y = waterLevel - 10; y <= info.groundLevel; y++) {
                if (y < driver.world.getBottomY()) continue;
                driver.setBlockNoNeighbors(7, y, 7, support);
                driver.setBlockNoNeighbors(7, y, 8, support);
                driver.setBlockNoNeighbors(8, y, 7, support);
                driver.setBlockNoNeighbors(8, y, 8, support);
            }
        }
        
        // Соединение с городом на концах. Оригинал: info.profile.GROUNDLEVEL (не info.groundLevel).
        if (xOrientation) {
            if (minBridge == null) {
                for (int z = 6; z <= 9; z++) {
                    driver.setBlockNoNeighbors(0, profileGround, z, support);
                }
            }
            if (maxBridge == null) {
                for (int z = 6; z <= 9; z++) {
                    driver.setBlockNoNeighbors(15, profileGround, z, support);
                }
            }
        } else {
            if (minBridge == null) {
                for (int x = 6; x <= 9; x++) {
                    driver.setBlockNoNeighbors(x, profileGround, 0, support);
                }
            }
            if (maxBridge == null) {
                for (int x = 6; x <= 9; x++) {
                    driver.setBlockNoNeighbors(x, profileGround, 15, support);
                }
            }
        }
    }

    /** Проверяет, пустой ли блок (для опор). */
    public static boolean isEmpty(BlockState state) {
        return state.isAir() || state.getBlock() == Blocks.STRUCTURE_VOID
            || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA;
    }
}
