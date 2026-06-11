package com.lostcity;

import com.lostcity.assets.AssetLoader;
import com.lostcity.config.ConfigManager;
import com.lostcity.config.LostCityConfig;
import com.lostcity.util.ModLogger;
import com.lostcity.worldgen.BuildingInfo;
import com.lostcity.worldgen.City;
import com.lostcity.worldgen.ModFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс мода Lost City для Fabric 1.20.1
 * Портирован с Forge версии McJtyMods/LostCities
 *
 * Этот класс инициализирует серверную и общую часть мода.
 */
public class LostCityMod implements ModInitializer {

    public static final String MOD_ID = "lostcities";
    public static final String MOD_NAME = "Lost City";
    public static final String VERSION = "0.7.0-beta";

    /** Логгер для всего мода */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    /**
     * Глобальная конфигурация мода
     */
    private static LostCityConfig config;

    /**
     * Получить конфигурацию мода
     */
    public static LostCityConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        ModLogger.logInitStart();

        // Шаг 1 - Инициализация стандартных профилей
        ModLogger.info("Initializing standard profiles...");
        LostCityConfig.init();

        // Шаг 2 - Инициализация конфигурации
        ModLogger.info("Loading configuration...");
        config = ConfigManager.load();
        config.printConfigInfo();

        // Инициализируем ProfileSelection из конфига (для совместимости с GUI)
        if (config != null && config.selectedProfile != null && !"disabled".equals(config.selectedProfile)) {
            // Инициализируем только на клиенте (GUI доступен только на клиенте)
            try {
                Class<?> profileSelectionClass = Class.forName("com.lostcity.gui.ProfileSelection");
                java.lang.reflect.Method setMethod = profileSelectionClass.getMethod("setSelectedProfile", String.class);
                setMethod.invoke(null, config.selectedProfile);
                ModLogger.info("ProfileSelection initialized from config: {}", config.selectedProfile);
            } catch (Exception e) {
                // На сервере GUI недоступен - это нормально
                ModLogger.debug("ProfileSelection not available (server side): {}", e.getMessage());
            }
        }

        // Шаг 3 - Регистрация Features
        ModLogger.info("Registering world generation features...");
        ModFeatures.registerFeature();
        ModFeatures.addFeatureToBiomes();
        ModLogger.info("World generation features registered");

        // Шаг 6 - Регистрация загрузчика ассетов (JSON: buildings, palettes, parts)
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new AssetLoader());
        ModLogger.info("Asset loader registered (loads on resource reload / world load)");

        // Шаг 11 - Очистка кэшей при выгрузке мира (как в оригинале ForgeEventHandlers)
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            BuildingInfo.cleanCache();
            City.clearCache();
        });

        ModLogger.logInitComplete();
    }

    /** Получить ID мода */
    public static String getModId() {
        return MOD_ID;
    }
}
