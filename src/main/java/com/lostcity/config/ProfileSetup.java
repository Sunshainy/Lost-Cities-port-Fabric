package com.lostcity.config;

import com.lostcity.LostCityMod;
import com.lostcity.util.ModLogger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Настройка стандартных профилей генерации
 * Портирован из mcjty.lostcities.config.ProfileSetup
 * 
 * Временно оставлен активным только базовый профиль "default" для стабильности.
 * Остальные профили закомментированы до завершения Этапа 2.
 */
public class ProfileSetup {

    public static final Map<String, ProfileConfig> STANDARD_PROFILES = new LinkedHashMap<>();

    public static void initStandardProfiles() {
        ModLogger.info("Initializing standard profiles...");
        
        ProfileConfig profile;

        // === РАБОЧИЕ ПРОФИЛИ ===

        // Профиль "default" - стандартная генерация городов
        profile = new ProfileConfig("default");
        profile.landscapeType = LandscapeType.DEFAULT.getName();
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // === НЕАКТИВНЫЕ ПРОФИЛИ (Будут включены в Этапе 2) ===
        /*
        // Профиль "cavern" - города в пещерах
        profile = new ProfileConfig("cavern");
        profile.landscapeType = LandscapeType.CAVERN.getName();
        profile.groundLevel = 40;
        profile.seaLevel = 32;
        profile.cityLevel0Height = 40 + 4;
        profile.cityLevel1Height = 40 + 12;
        profile.cityLevel2Height = 40 + 20;
        profile.cityLevel3Height = 40 + 28;
        profile.cityLevel4Height = 40 + 36;
        profile.cityLevel5Height = 40 + 42;
        profile.cityLevel6Height = 40 + 50;
        profile.cityLevel7Height = 40 + 58;
        profile.explosionChance = 0.0f;
        profile.miniExplosionChance = 0.0f;
        profile.generateLighting = true;
        profile.railwaysEnabled = false;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "nodamage" - без взрывов и разрушений
        profile = new ProfileConfig("nodamage");
        profile.explosionChance = 0.0f;
        profile.miniExplosionChance = 0.0f;
        profile.ruinChance = 0.0f;
        profile.rubbleLayer = false;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "floating" - города на плавающих островах
        profile = new ProfileConfig("floating");
        profile.landscapeType = LandscapeType.FLOATING.getName();
        profile.cityChance = 0.03f;
        profile.highwaySupports = false;
        profile.buildingMaxCellars = 1;
        profile.railwaysCanEnd = true;
        profile.railwaysEnabled = false;
        profile.railwayStationsEnabled = false;
        profile.highwayDistanceMask = 15;
        profile.groundLevel = 50;
        profile.cityLevel0Height = 50;
        profile.cityLevel1Height = 56;
        profile.cityLevel2Height = 62;
        profile.cityLevel3Height = 68;
        profile.cityLevel4Height = 76;
        profile.cityLevel5Height = 84;
        profile.cityLevel6Height = 92;
        profile.cityLevel7Height = 100;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "space" - города в стеклянных сферах
        profile = new ProfileConfig("space");
        profile.landscapeType = LandscapeType.SPACE.getName();
        profile.citySphereOutsideProfile = "void_outside";
        profile.spawnSphere = "<in>";
        profile.citySphereChance = 0.9f;
        profile.citySphereClearAbove = 8;
        profile.citySphereClearAboveUntilAir = true;
        profile.citySphereClearBelow = 8;
        profile.citySphereClearBelowUntilAir = true;
        profile.railwaysCanEnd = true;
        profile.railwaysEnabled = false;
        profile.railwayStationsEnabled = false;
        profile.highwayDistanceMask = 0;
        profile.bridgeSupports = false;
        profile.highwaySupports = false;
        profile.rubbleLayer = false;
        profile.groundLevel = 71;
        profile.explosionChance = 0.0001f;
        profile.miniExplosionChance = 0.001f;
        profile.cityChance = 0.7f;
        profile.cityMaxRadius = 90;
        profile.cityThreshold = 0.05f;
        profile.cityLevel0Height = 60;
        profile.cityLevel1Height = 66;
        profile.cityLevel2Height = 72;
        profile.cityLevel3Height = 78;
        profile.cityLevel4Height = 86;
        profile.cityLevel5Height = 94;
        profile.cityLevel6Height = 100;
        profile.cityLevel7Height = 108;
        profile.buildingChance = 0.3f;
        profile.generateLighting = true;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "biosphere_caves" - сферы в больших пещерах
        profile = new ProfileConfig("biosphere_caves");
        profile.landscapeType = LandscapeType.CAVERNSPHERES.getName();
        profile.citySphereMonorailChance = 0.0f;
        profile.citySphereOutsideProfile = "cavern";
        profile.citySphereOutsideSurfaceVariation = 0.5f;
        profile.spawnSphere = "<in>";
        profile.explosionChance = 0.0f;
        profile.miniExplosionChance = 0.01f;
        profile.miniExplosionMinHeight = 60;
        profile.miniExplosionMaxHeight = 75;
        profile.miniExplosionMinRadius = 5;
        profile.miniExplosionMaxRadius = 10;
        profile.railwaysCanEnd = true;
        profile.railwaysEnabled = false;
        profile.railwayStationsEnabled = false;
        profile.highwayDistanceMask = 0;
        profile.ruinChance = 0.7f;
        profile.ruinMinLevelPercent = 0.3f;
        profile.ruinMaxLevelPercent = 0.8f;
        profile.rubbleLayer = false;
        profile.citySphereChance = 0.5f;
        profile.citySphereClearAbove = 30;
        profile.cityChance = 0.9f;
        profile.cityMinRadius = 50;
        profile.cityMaxRadius = 65;
        profile.cityThreshold = 0.05f;
        profile.cityLevel0Height = 60;
        profile.cityLevel1Height = 66;
        profile.cityLevel2Height = 72;
        profile.cityLevel3Height = 78;
        profile.cityLevel4Height = 86;
        profile.cityLevel5Height = 94;
        profile.cityLevel6Height = 100;
        profile.cityLevel7Height = 108;
        profile.buildingChance = 0.3f;
        profile.generateLighting = true;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "biosphere" - джунгли в стеклянных сферах на пустынном ландшафте
        profile = new ProfileConfig("biosphere");
        profile.landscapeType = LandscapeType.SPHERES.getName();
        profile.citySphereMonorailChance = 0.0f;
        profile.citySphereOutsideProfile = "bio_wasteland";
        profile.citySphereOutsideSurfaceVariation = 0.5f;
        profile.spawnSphere = "<in>";
        profile.explosionChance = 0.0f;
        profile.miniExplosionChance = 0.01f;
        profile.miniExplosionMinHeight = 60;
        profile.miniExplosionMaxHeight = 75;
        profile.miniExplosionMinRadius = 5;
        profile.miniExplosionMaxRadius = 10;
        profile.railwaysCanEnd = true;
        profile.railwaysEnabled = false;
        profile.railwayStationsEnabled = false;
        profile.highwayDistanceMask = 0;
        profile.ruinChance = 0.7f;
        profile.ruinMinLevelPercent = 0.3f;
        profile.ruinMaxLevelPercent = 0.8f;
        profile.rubbleLayer = false;
        profile.citySphereChance = 0.5f;
        profile.citySphereClearAbove = 30;
        profile.cityChance = 0.8f;
        profile.cityMinRadius = 50;
        profile.cityMaxRadius = 65;
        profile.cityThreshold = 0.05f;
        profile.cityLevel0Height = 60;
        profile.cityLevel1Height = 66;
        profile.cityLevel2Height = 72;
        profile.cityLevel3Height = 78;
        profile.cityLevel4Height = 86;
        profile.cityLevel5Height = 94;
        profile.cityLevel6Height = 100;
        profile.cityLevel7Height = 108;
        profile.buildingChance = 0.3f;
        profile.generateLighting = true;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "rarecities" - редкие города
        profile = new ProfileConfig("rarecities");
        profile.cityChance = 0.001f;
        profile.ruinChance = 0.0f;
        profile.highwayRequiresTwoCities = false;
        profile.railwaysCanEnd = true;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "onlycities" - весь мир - город
        profile = new ProfileConfig("onlycities");
        profile.cityChance = 0.2f;
        profile.cityMaxRadius = 256;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "tallbuildings" - очень высокие здания
        profile = new ProfileConfig("tallbuildings");
        profile.buildingMinFloors = 4;
        profile.buildingMinFloorsChance = 8;
        profile.buildingMaxFloorsChance = 15;
        profile.buildingMaxFloors = 19;
        profile.debrisToNearbyChunkFactor = 175;
        profile.explosionChance = 0.006f;
        profile.explosionMaxHeight = 256;
        profile.explosionMaxRadius = 60;
        profile.explosionMinHeight = 130;
        profile.miniExplosionChance = 0.09f;
        profile.miniExplosionMaxHeight = 256;
        profile.miniExplosionMaxRadius = 14;
        profile.miniExplosionMinRadius = 3;
        profile.ruinChance = 0.01f;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "safe" - безопасный режим: без спавнеров, освещение но без лута
        profile = new ProfileConfig("safe");
        profile.generateSpawners = false;
        profile.generateLighting = true;
        profile.generateLoot = false;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "ancient" - древний джунглевый город, лианы и листья, разрушенные здания
        profile = new ProfileConfig("ancient");
        profile.thicknessOfRandomLeafBlocks = 6;
        profile.chanceOfRandomLeafBlocks = 0.05f;
        profile.vineChance = 0.1f;
        profile.explosionChance = 0.0f;
        profile.miniExplosionChance = 0.0f;
        profile.rubbleLayer = true;
        profile.rubbleDirtScale = 2.0f;
        profile.rubbleLeaveScale = 2.0f;
        profile.ruinChance = 0.9f;
        profile.ruinMinLevelPercent = 0.0f;
        profile.ruinMaxLevelPercent = 0.9f;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "wasteland" - пустошь, без воды, голый ландшафт
        profile = new ProfileConfig("wasteland");
        profile.vineChance = 0.003f;
        profile.chanceOfRandomLeafBlocks = 0.01f;
        profile.rubbleLayer = true;
        profile.rubbleDirtScale = 2.0f;
        profile.rubbleLeaveScale = 0.0f;
        profile.ruinChance = 0.5f;
        profile.ruinMinLevelPercent = 0.5f;
        profile.ruinMaxLevelPercent = 0.9f;
        profile.avoidWater = true;
        profile.avoidFoliage = true;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "atlantis" - затопленные города, повышенный уровень воды (до 89)
        profile = new ProfileConfig("atlantis");
        profile.seaLevel = 89;
        profile.ruinChance = 0.1f;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);

        // Профиль "largecities" - большие города
        profile = new ProfileConfig("largecities");
        profile.cityChance = -1.0f; // Использует Perlin noise
        profile.cityPerlinScale = 7.0;
        profile.cityPerlinOffset = 0.2;
        profile.cityPerlinInnerScale = 0.1;
        profile.cityThreshold = 0.1f;
        profile.cityStyleThreshold = 0.4f;
        profile.cityStyleAlternative = "citystyle_border";
        profile.generateLighting = true;
        profile.buildingMaxFloors = 9;
        profile.buildingMaxFloorsChance = 7;
        profile.buildingChance = 0.4f;
        STANDARD_PROFILES.put(profile.getProfileName(), profile);
        */

        ModLogger.info("Initialized {} standard profiles: {}", STANDARD_PROFILES.size(), 
            String.join(", ", STANDARD_PROFILES.keySet()));
    }

    public static ProfileConfig getStandardProfile(String name) {
        return STANDARD_PROFILES.get(name);
    }

    public static boolean hasStandardProfile(String name) {
        return STANDARD_PROFILES.containsKey(name);
    }
}
