package com.lostcity.config;

/**
 * Профиль генерации города - содержит все параметры для конкретного профиля.
 * Портирован из LostCityProfile (оригинальный Forge мод).
 */
public class ProfileConfig {
    
    // === ОСНОВНЫЕ ПАРАМЕТРЫ ===
    
    public String profileName = "default";
    public float cityChance = 0.05f;
    public int groundLevel = 71;
    public int seaLevel = -1;

    public int cityLevel0Height = 75;
    public int cityLevel1Height = 83;
    public int cityLevel2Height = 91;
    public int cityLevel3Height = 99;
    public int cityLevel4Height = 107;
    public int cityLevel5Height = 115;
    public int cityLevel6Height = 123;
    public int cityLevel7Height = 131;

    public int parkStreetThreshold = 3;

    public int highwayDistanceMask = 7;
    public float highwayMainPerlinScale = 50.0f;
    public float highwaySecondaryPerlinScale = 10.0f;
    public float highwayPerlinFactor = 2.0f;
    public boolean highwayRequiresTwoCities = true;
    public int highwayLevelFromCitiesMode = 0;
    public boolean highwaySupports = true;

    // === МОСТИКИ (Bridges) ===
    public float bridgeChance = 0.7f;
    public boolean bridgeSupports = true;

    public int cityMaxRadius = 128;
    public int cityMinRadius = 50;
    public int cityMinHeight = 0;
    public int cityMaxHeight = 1000;
    public int citySpawnDistance1 = 0;
    public int citySpawnDistance2 = 0;
    public float citySpawnMultiplier1 = 1.0f;
    public float citySpawnMultiplier2 = 1.0f;
    public float cityThreshold = 0.2f;
    public double cityPerlinScale = 100.0;
    public double cityPerlinOffset = 0.0;
    public double cityPerlinInnerScale = 1.0;
    public float cityStyleThreshold = -1.0f;
    public String cityStyleAlternative = "";
    
    public int buildingMinFloors = 0;
    public int buildingMaxFloors = 9;
    public int buildingMaxCellars = 6;
    public int buildingFloorHeight = 6;
    public float buildingChance = 0.3f;
    public float buildingDoorwayChance = 0.7f;
    public float buildingFrontChance = 0.2f;

    public float parkChance = 0.2f;
    public float corridorChance = 0.7f;
    public float fountainChance = 0.05f;
    public float chestWithoutLootChance = 0.2f;
    public float buildingWithoutLootChance = 0.2f;
    public float railwayDungeonChance = 0.01f;
    public float scatteredChanceMultiplier = 1.0f;
    public int buildingMinCellars = 0;
    public int buildingMinFloorsChance = 4;
    public int buildingMaxFloorsChance = 6;

    public boolean railwaySurfaceStationsEnabled = true;
    public boolean parkElevation = true;
    public boolean parkBorder = true;
    public float vineChance = 0.009f;

    // === СФЕРЫ И МОНОРЕЛЬСЫ (Spheres & Monorails) ===
    public float citySphereFactor = 1.2f;
    public float citySphereChance = 0.1f;
    public boolean citySphere32Grid = false;
    public boolean citySphereOnlyPredefined = false;
    public float citySphereMonorailChance = 0.8f;
    public int citySphereMonorailHeightOffset = -2;
    public float citySphereSurfaceOffset = 0.1f;
    public int citySphereOuterGroundLevel = 35;

    public float chanceOfRandomLeafBlocks = 0.05f;
    public int thicknessOfRandomLeafBlocks = 2;
    public boolean avoidFoliage = false;

    public boolean generateLoot = true;
    public boolean generateSpawners = true;
    public boolean generateLighting = false;
    public boolean avoidWater = false;
    
    public float explosionChance = 0.002f;
    public int explosionMinRadius = 15;
    public int explosionMaxRadius = 35;
    public int explosionMinHeight = 75;
    public int explosionMaxHeight = 90;
    
    public float miniExplosionChance = 0.03f;
    public int miniExplosionMinRadius = 5;
    public int miniExplosionMaxRadius = 12;
    public int miniExplosionMinHeight = 60;
    public int miniExplosionMaxHeight = 100;
    
    public float ruinChance = 0.05f;
    public float ruinMinLevelPercent = 0.8f;
    public float ruinMaxLevelPercent = 1.0f;
    
    public boolean rubbleLayer = true;
    public float rubbleDirtScale = 3.0f;
    public float rubbleLeaveScale = 6.0f;
    public int debrisToNearbyChunkFactor = 200;
    
    public boolean railwaysCanEnd = false;
    public boolean railwaysEnabled = true;
    public boolean railwayStationsEnabled = true;
    
    public String citySphereOutsideProfile = "";
    public float citySphereOutsideSurfaceVariation = 1.0f;
    public String spawnSphere = "";
    
    public int citySphereClearAbove = 0;
    public boolean citySphereClearAboveUntilAir = false;
    public int citySphereClearBelow = 0;
    public boolean citySphereClearBelowUntilAir = false;

    // === TERRAIN CORRECTION ===
    public int terrainFixLowerMinOffset = -4;
    public int terrainFixLowerMaxOffset = -3;
    public int terrainFixUpperMinOffset = -1;
    public int terrainFixUpperMaxOffset = 1;
    public int oceanCorrectionBorder = 0;

    // === CITY LEVEL - ПРОФИЛИ ===
    public String landscapeType = "default";
    public boolean useAvgHeightmap = false;
    public int heightSampleSize = 4;
    public boolean cityAvoidVoid = true;
    public boolean multiUseCorner = false;

    public ProfileConfig() {}
    
    public ProfileConfig(String name) {
        this.profileName = name;
    }
    
    public String getProfileName() { return profileName; }
    public float getCityChance() { return cityChance; }
    public int getGroundLevel() { return groundLevel; }
    public int getCityMaxRadius() { return cityMaxRadius; }
    public int getCityMinRadius() { return cityMinRadius; }
    public int getBuildingMinFloors() { return buildingMinFloors; }
    public int getBuildingMaxFloors() { return buildingMaxFloors; }
    public int getBuildingMaxCellars() { return buildingMaxCellars; }
    public int getBuildingFloorHeight() { return buildingFloorHeight; }
    public float getBuildingChance() { return buildingChance; }
    public float getBuildingDoorwayChance() { return buildingDoorwayChance; }
    public float getBuildingFrontChance() { return buildingFrontChance; }
    public float getParkChance() { return parkChance; }
    public float getCorridorChance() { return corridorChance; }
    public float getFountainChance() { return fountainChance; }
    public float getChestWithoutLootChance() { return chestWithoutLootChance; }
    public float getBuildingWithoutLootChance() { return buildingWithoutLootChance; }
    public float getRailwayDungeonChance() { return railwayDungeonChance; }
    public float getScatteredChanceMultiplier() { return scatteredChanceMultiplier; }
    public int getBuildingMinCellars() { return buildingMinCellars; }
    public int getBuildingMinFloorsChance() { return buildingMinFloorsChance; }
    public int getBuildingMaxFloorsChance() { return buildingMaxFloorsChance; }
    public boolean getRailwaySurfaceStationsEnabled() { return railwaySurfaceStationsEnabled; }
    public boolean getParkElevation() { return parkElevation; }
    public boolean getParkBorder() { return parkBorder; }

    public float getCitySphereFactor() { return citySphereFactor; }
    public float getCitySphereChance() { return citySphereChance; }
    public boolean getCitySphere32Grid() { return citySphere32Grid; }
    public boolean getCitySphereOnlyPredefined() { return citySphereOnlyPredefined; }
    public float getCitySphereMonorailChance() { return citySphereMonorailChance; }
    public int getCitySphereMonorailHeightOffset() { return citySphereMonorailHeightOffset; }
    public float getCitySphereSurfaceOffset() { return citySphereSurfaceOffset; }
    public int getCitySphereOuterGroundLevel() { return citySphereOuterGroundLevel; }

    public boolean getGenerateLoot() { return generateLoot; }
    public boolean getGenerateSpawners() { return generateSpawners; }
    public int getSeaLevel() { return seaLevel; }
    public int getCityLevel0Height() { return cityLevel0Height; }
    public int getCityLevel1Height() { return cityLevel1Height; }
    public int getCityLevel2Height() { return cityLevel2Height; }
    public int getCityLevel3Height() { return cityLevel3Height; }
    public int getCityLevel4Height() { return cityLevel4Height; }
    public int getCityLevel5Height() { return cityLevel5Height; }
    public int getCityLevel6Height() { return cityLevel6Height; }
    public int getCityLevel7Height() { return cityLevel7Height; }
    public int getParkStreetThreshold() { return parkStreetThreshold; }
    public int getHighwayDistanceMask() { return highwayDistanceMask; }
    public float getHighwayMainPerlinScale() { return highwayMainPerlinScale; }
    public float getHighwaySecondaryPerlinScale() { return highwaySecondaryPerlinScale; }
    public float getHighwayPerlinFactor() { return highwayPerlinFactor; }
    public boolean getHighwayRequiresTwoCities() { return highwayRequiresTwoCities; }
    public int getHighwayLevelFromCitiesMode() { return highwayLevelFromCitiesMode; }
    public boolean getHighwaySupports() { return highwaySupports; }
    public float getBridgeChance() { return bridgeChance; }
    public boolean getBridgeSupports() { return bridgeSupports; }
    
    public int getCityMinHeight() { return cityMinHeight; }
    public int getCityMaxHeight() { return cityMaxHeight; }
    public int getCitySpawnDistance1() { return citySpawnDistance1; }
    public int getCitySpawnDistance2() { return citySpawnDistance2; }
    public float getCitySpawnMultiplier1() { return citySpawnMultiplier1; }
    public float getCitySpawnMultiplier2() { return citySpawnMultiplier2; }
    public float getCityThreshold() { return cityThreshold; }
    public double getCityPerlinScale() { return cityPerlinScale; }
    public double getCityPerlinOffset() { return cityPerlinOffset; }
    public double getCityPerlinInnerScale() { return cityPerlinInnerScale; }
    public float getCityStyleThreshold() { return cityStyleThreshold; }
    public String getCityStyleAlternative() { return cityStyleAlternative; }
    
    public int getTerrainFixLowerMinOffset() { return terrainFixLowerMinOffset; }
    public int getTerrainFixLowerMaxOffset() { return terrainFixLowerMaxOffset; }
    public int getTerrainFixUpperMinOffset() { return terrainFixUpperMinOffset; }
    public int getTerrainFixUpperMaxOffset() { return terrainFixUpperMaxOffset; }
    public int getOceanCorrectionBorder() { return oceanCorrectionBorder; }
    
    public String getLandscapeType() { return landscapeType; }
    public boolean getUseAvgHeightmap() { return useAvgHeightmap; }
    public int getHeightSampleSize() { return heightSampleSize; }
    public boolean getCityAvoidVoid() { return cityAvoidVoid; }
    
    public boolean isSpace() { return "space".equalsIgnoreCase(landscapeType); }
    public boolean isFloating() { return "floating".equalsIgnoreCase(landscapeType); }
    public boolean isCavern() { return "cavern".equalsIgnoreCase(landscapeType); }
    public boolean isDefault() { return "default".equalsIgnoreCase(landscapeType); }
    public boolean isSpheres() { return isSpace() || "spheres".equalsIgnoreCase(landscapeType); }

    public boolean getRailwaysEnabled() { return railwaysEnabled; }
    public boolean getRailwayStationsEnabled() { return railwayStationsEnabled; }

    public boolean getMultiUseCorner() { return multiUseCorner; }

    public int getLevelBasedOnHeight(int height) {
        if (height < cityLevel0Height) return 0;
        if (height < cityLevel1Height) return 1;
        if (height < cityLevel2Height) return 2;
        if (height < cityLevel3Height) return 3;
        if (height < cityLevel4Height) return 4;
        if (height < cityLevel5Height) return 5;
        if (height < cityLevel6Height) return 6;
        if (height < cityLevel7Height) return 7;
        return 8;
    }

    @Override
    public String toString() {
        return String.format("ProfileConfig[%s]: cityChance=%.3f, groundLevel=%d, cityRadius=%d-%d",
            profileName, cityChance, groundLevel, cityMinRadius, cityMaxRadius);
    }
}
