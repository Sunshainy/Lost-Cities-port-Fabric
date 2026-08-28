package com.lostcity.worldgen;

import com.lostcity.LostCityMod;
import com.lostcity.util.ModLogger;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Регистрация Feature для Lost City
 * FABRIC VERSION с правильными импортами
 */
public class ModFeatures {
    
    // Сам Feature
    public static Feature<DefaultFeatureConfig> LOST_CITY_FEATURE;
    
    // Ключ для PlacedFeature
    public static final RegistryKey<PlacedFeature> LOST_CITY_PLACED_KEY = 
        RegistryKey.of(RegistryKeys.PLACED_FEATURE, 
            Identifier.of(LostCityMod.MOD_ID, "city_generator"));
    
    /**
     * Регистрация Feature
     * Вызывается из LostCityMod.onInitialize()
     */
    public static void registerFeature() {
        ModLogger.info("Registering LostCityFeature...");
        
        // Создаём и регистрируем Feature
        LOST_CITY_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(LostCityMod.MOD_ID, "city_generator"),
            new LostCityFeature(DefaultFeatureConfig.CODEC)
        );
        
        ModLogger.info("LostCityFeature registered successfully");
    }
    
    /**
     * Добавление Feature в биомы
     * Вызывается из LostCityMod.onInitialize()
     */
    public static void addFeatureToBiomes() {
        ModLogger.info("Adding LostCityFeature to biomes...");
        
        // Добавляем Feature во все биомы Overworld
        BiomeModifications.addFeature(
            // Selector - выбираем только Overworld биомы
            BiomeSelectors.foundInOverworld(),
            
            // Этап генерации - RAW_GENERATION (самый первый этап)
            GenerationStep.Feature.RAW_GENERATION,
            
            // Ключ нашего PlacedFeature
            LOST_CITY_PLACED_KEY
        );
        
        ModLogger.info("LostCityFeature added to all Overworld biomes");
        ModLogger.info("Generation step: RAW_GENERATION");
    }
    
    /**
     * Получить зарегистрированный Feature
     */
    public static Feature<DefaultFeatureConfig> getFeature() {
        return LOST_CITY_FEATURE;
    }
}
