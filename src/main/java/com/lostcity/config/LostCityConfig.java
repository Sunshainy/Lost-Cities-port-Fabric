package com.lostcity.config;

import com.lostcity.util.ModLogger;

/**
 * Главный класс конфигурации мода Lost City
 * Содержит все настройки и профили генерации
 * 
 * Портирован из Config.java (оригинальный Forge мод)
 */
public class LostCityConfig {
    
    // === ОБЩИЕ НАСТРОЙКИ ===
    
    /**
     * Активный профиль для Overworld
     * По умолчанию: "default"
     * Возможные значения: "disabled", "default", или другой профиль
     */
    public String selectedProfile = "default";
    
    /**
     * Включить отладочное логирование генерации
     */
    public boolean debugLogging = true;

    /**
     * Время жизни кэша (секунды). После N сек неиспользования запись удаляется.
     * Оригинал: CACHE_CLEANUP_SECONDS. По умолчанию 300 (5 мин).
     */
    public int cacheCleanupSeconds = 300;
    
    // === ПРОФИЛИ ===
    
    /**
     * Профиль "default" - стандартная генерация городов
     * Используется как fallback, если стандартные профили не загружены
     */
    private ProfileConfig defaultProfile = new ProfileConfig("default");
    
    /**
     * Инициализация стандартных профилей
     * Вызывается при загрузке мода
     */
    public static void init() {
        ProfileSetup.initStandardProfiles();
    }
    
    // === МЕТОДЫ ДОСТУПА ===
    
    /**
     * Получить активный профиль для генерации
     */
    public ProfileConfig getActiveProfile() {
        if ("disabled".equals(selectedProfile)) {
            return null; // Vanilla generation
        }
        
        ProfileConfig profile = ProfileSetup.getStandardProfile(selectedProfile);
        if (profile != null) {
            return profile;
        }
        
        ModLogger.warn("Profile '{}' not found in STANDARD_PROFILES, using 'default'", selectedProfile);
        return ProfileSetup.getStandardProfile("default");
    }
    
    /**
     * Получить профиль по имени
     */
    public ProfileConfig getProfileByName(String name) {
        if ("disabled".equals(name)) {
            return null;
        }
        
        ProfileConfig profile = ProfileSetup.getStandardProfile(name);
        if (profile != null) {
            return profile;
        }
        
        ModLogger.warn("Profile '{}' not found, using 'default'", name);
        return ProfileSetup.getStandardProfile("default");
    }
    
    /**
     * Включено ли отладочное логирование
     */
    public boolean isDebugLogging() {
        return debugLogging;
    }

    public int getCacheCleanupSeconds() {
        return cacheCleanupSeconds;
    }
    
    /**
     * Получить Map всех доступных профилей
     * Возвращает все стандартные профили из ProfileSetup
     */
    public java.util.Map<String, ProfileConfig> getProfiles() {
        return new java.util.HashMap<>(ProfileSetup.STANDARD_PROFILES);
    }
    
    /**
     * Получить профиль по имени (альтернативный метод)
     */
    public ProfileConfig getProfile(String name) {
        return getProfileByName(name);
    }
    
    /**
     * Вывести информацию о конфигурации
     */
    public void printConfigInfo() {
        ModLogger.info("=== LOST CITY CONFIGURATION ===");
        ModLogger.info("Selected Profile: {}", selectedProfile);
        ModLogger.info("Debug Logging: {}", debugLogging);
        ModLogger.info("Active Profile: {}", getActiveProfile());
        ModLogger.info("===============================");
    }
}
