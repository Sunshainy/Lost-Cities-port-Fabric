package com.lostcity.worldgen;

import com.lostcity.assets.CompiledPalette;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class Corridors {
    public static void generateCorridors(LostCityFeature feature, ChunkDriver driver, BuildingInfo info, boolean xRail, boolean zRail) {
        BlockState air = Blocks.AIR.getDefaultState();
        // В оригинале base берется из profile.getBaseBlock(), используем STONE как дефолт
        BlockState base = Blocks.STONE.getDefaultState();
        BlockState railx = Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST);
        BlockState railz = Blocks.RAIL.getDefaultState();

        com.lostcity.assets.CityStyle cityStyle = City.getCityStyle(info.chunkPos, info.config, driver.world);
        Character corridorRoofBlock = cityStyle != null ? cityStyle.getCorridorRoofBlock() : null;
        Character corridorGlassBlock = cityStyle != null ? cityStyle.getCorridorGlassBlock() : null;
        CompiledPalette palette = info.getCompiledPalette(driver.world);

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                BlockState b;
                if ((xRail && z >= 7 && z <= 10) || (zRail && x >= 7 && x <= 10)) {
                    int height = info.groundLevel - 6;
                    if (xRail && z == 10) {
                        b = railx;
                    } else if (zRail && x == 10) {
                        b = railz;
                    } else {
                        b = air;
                    }
                    
                    driver.setBlockNoNeighbors(x, height, z, palette.get(corridorRoofBlock) != null ? palette.get(corridorRoofBlock) : base);
                    driver.setBlockNoNeighbors(x, height + 1, z, b);
                    driver.setBlockNoNeighbors(x, height + 2, z, air);
                    driver.setBlockNoNeighbors(x, height + 3, z, air);

                    if ((xRail && x == 7 && (z == 8 || z == 9)) || (zRail && z == 7 && (x == 8 || x == 9))) {
                        driver.setBlockNoNeighbors(x, height + 4, z, palette.get(corridorGlassBlock) != null ? palette.get(corridorGlassBlock) : Blocks.GLASS.getDefaultState());
                        
                        Character glowstoneChar = cityStyle != null ? cityStyle.getGlowstoneBlock() : null;
                        BlockState glowstone = glowstoneChar == null ? Blocks.GLOWSTONE.getDefaultState() : palette.get(glowstoneChar);
                        if (glowstone == null) glowstone = Blocks.GLOWSTONE.getDefaultState();
                        
                        driver.setBlockNoNeighbors(x, height + 5, z, glowstone);
                    } else {
                        BlockState roof = palette.get(corridorRoofBlock) != null ? palette.get(corridorRoofBlock) : base;
                        driver.setBlockNoNeighbors(x, height + 4, z, roof);
                        driver.setBlockNoNeighbors(x, height + 5, z, roof);
                    }
                } else {
                    for (int y = info.groundLevel - 5; y <= info.getCityGroundLevel(); y++) {
                        driver.setBlockNoNeighbors(x, y, z, base);
                    }
                }
            }
        }
    }

    public static void generateCorridorConnections(ChunkDriver driver, BuildingInfo info) {
        if (info.getXmin() != null && info.getXmin().hasXCorridor()) {
            int x = 0;
            for (int z = 7; z <= 10; z++) {
                for (int y = info.groundLevel - 5; y <= info.groundLevel - 2; y++) {
                    driver.setBlockNoNeighbors(x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
        if (info.getXmax() != null && info.getXmax().hasXCorridor()) {
            int x = 15;
            for (int z = 7; z <= 10; z++) {
                for (int y = info.groundLevel - 5; y <= info.groundLevel - 2; y++) {
                    driver.setBlockNoNeighbors(x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
        if (info.getZmin() != null && info.getZmin().hasZCorridor()) {
            int z = 0;
            for (int x = 7; x <= 10; x++) {
                for (int y = info.groundLevel - 5; y <= info.groundLevel - 2; y++) {
                    driver.setBlockNoNeighbors(x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
        if (info.getZmax() != null && info.getZmax().hasZCorridor()) {
            int z = 15;
            for (int x = 7; x <= 10; x++) {
                for (int y = info.groundLevel - 5; y <= info.groundLevel - 2; y++) {
                    driver.setBlockNoNeighbors(x, y, z, Blocks.AIR.getDefaultState());
                }
            }
        }
    }
}