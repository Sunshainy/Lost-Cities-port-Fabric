package com.lostcity.worldgen;

import com.lostcity.assets.AssetRegistries;
import com.lostcity.assets.CompiledPalette;
import com.lostcity.assets.StuffObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import com.lostcity.util.QualityRandom;

import java.util.List;
import java.util.Random;

public class Stuff {

    public static void generateStuff(LostCityFeature feature, ChunkDriver driver, BuildingInfo info) {
        long seed = info.chunkPos.x * 2570174657L + info.chunkPos.z * 101754695981L;
        Random rand = new QualityRandom(seed);
        
        com.lostcity.assets.CityStyle cityStyle = City.getCityStyle(info.chunkPos, info.config, driver.world);
        if (cityStyle == null) return;
        
        CompiledPalette palette = info.getCompiledPalette(driver.world);
        // BiomeInfo is simplified here; we'll use a fast world check for Biome
        net.minecraft.world.biome.Biome biome = driver.world.getBiome(new BlockPos(info.chunkPos.getStartX() + 8, info.groundLevel, info.chunkPos.getStartZ() + 8)).value();

        for (String tag : cityStyle.getStuffTags()) {
            List<StuffObject> stuffs = AssetRegistries.getStuffByTag(tag);
            if (stuffs != null) {
                for (StuffObject stuff : stuffs) {
                    Boolean inBuilding = stuff.inbuilding;
                    if (inBuilding == null || inBuilding == info.hasBuilding) {
                        if (stuff.buildingMatcher.isAny() || stuff.buildingMatcher.test(info.buildingType, null, null)) {
                            if (stuff.biomeMatcher.isAny() || stuff.biomeMatcher.test(biome, null, null)) { // Need to get ID and tags for biome actually
                                net.minecraft.util.Identifier biomeId = driver.world.getRegistryManager()
                                    .getOptional(net.minecraft.registry.RegistryKeys.BIOME)
                                    .map(registry -> registry.getId(biome))
                                    .orElse(null);
                                if (stuff.biomeMatcher.test(biome, biomeId, null)) {
                                    actuallyGenerateStuff(feature, driver, info, stuff, palette, inBuilding == Boolean.TRUE, rand);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean testBlock(ChunkDriver driver, StuffObject.BlockMatcher matcher, int x, int y, int z) {
        if (matcher.isAny()) {
            return true;
        }
        return matcher.test(driver.getBlock(x, y, z));
    }

    private static void actuallyGenerateStuff(LostCityFeature feature, ChunkDriver driver, BuildingInfo info, StuffObject settings, CompiledPalette palette, boolean inBuilding, Random rand) {
        int attempts = settings.attempts;
        Integer minheight = settings.minheight;
        Integer maxheight = settings.maxheight;
        
        if (minheight == null) {
            minheight = info.groundLevel;
            if (inBuilding && info.hasBuilding) {
                int lowestLevel = info.getCityGroundLevel() - info.cellars * BuildingInfo.FLOOR_HEIGHT;
                minheight = lowestLevel;
            }
        }
        
        if (maxheight == null) {
            maxheight = minheight + 20;
            if (inBuilding && info.hasBuilding) {
                maxheight = info.getCityGroundLevel() + info.floors * BuildingInfo.FLOOR_HEIGHT + 10;
            }
        }
        
        if (maxheight <= minheight) {
            return;
        }
        
        int mincount = settings.mincount;
        int maxcount = settings.maxcount;
        int count = mincount;
        if (maxcount > mincount) {
            count = rand.nextInt(maxcount - mincount) + mincount;
        }
        
        BlockState air = Blocks.AIR.getDefaultState();
        
        for (int j = 0; j < count; j++) {
            for (int i = 0; i < attempts; i++) {
                int x = rand.nextInt(16);
                int y = rand.nextInt(maxheight - minheight) + minheight;
                int z = rand.nextInt(16);
                String blocks = settings.column;
                
                if (testBlock(driver, settings.blockMatcher, x, y - 1, z) && testBlock(driver, settings.upperBlockMatcher, x, y + blocks.length(), z)) {
                    Boolean isSeesky = settings.seesky;
                    
                    if (isSeesky == null || isSeesky == driver.world.isSkyVisible(new BlockPos(info.chunkPos.getStartX() + x, y, info.chunkPos.getStartZ() + z))) {
                        boolean ok = true;
                        for (int k = 0; k < blocks.length(); k++) {
                            if (driver.getBlock(x, y + k, z) != air) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            for (int k = 0; k < blocks.length(); k++) {
                                BlockState block = palette.get(blocks.charAt(k));
                                if (block != null) {
                                    driver.setBlockNoNeighbors(x, y + k, z, block);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}