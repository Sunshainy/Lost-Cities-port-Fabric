package com.lostcity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostcity.LostCityMod;
import com.lostcity.util.ModLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Менеджер для загрузки и сохранения конфигурации
 * Использует JSON для хранения настроек
 */
public class ConfigManager {
    
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "lostcity.json";
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    /**
     * Загрузить конфигурацию из файла или создать новую
     */
    public static LostCityConfig load() {
        Path configPath = getConfigPath();
        
        // Если файл существует - загружаем
        if (Files.exists(configPath)) {
            try {
                ModLogger.info("Loading config from: {}", configPath);
                String json = Files.readString(configPath);
                LostCityConfig config = GSON.fromJson(json, LostCityConfig.class);
                ModLogger.info("Config loaded successfully");
                return config;
            } catch (IOException e) {
                ModLogger.error("Failed to load config, using defaults: {}", e.getMessage());
                return createDefaultConfig();
            }
        } else {
            // Создаём новый конфиг с дефолтными значениями
            ModLogger.info("Config file not found, creating default config");
            LostCityConfig config = createDefaultConfig();
            save(config);
            return config;
        }
    }
    
    /**
     * Сохранить конфигурацию в файл
     */
    public static void save(LostCityConfig config) {
        Path configPath = getConfigPath();
        
        try {
            // Создаём директорию если её нет
            Files.createDirectories(configPath.getParent());
            
            // Сериализуем и сохраняем
            String json = GSON.toJson(config);
            Files.writeString(configPath, json);
            
            ModLogger.info("Config saved to: {}", configPath);
        } catch (IOException e) {
            ModLogger.error("Failed to save config: {}", e.getMessage());
        }
    }
    
    /**
     * Создать конфигурацию по умолчанию
     */
    private static LostCityConfig createDefaultConfig() {
        return new LostCityConfig();
    }
    
    /**
     * Получить путь к файлу конфигурации
     */
    private static Path getConfigPath() {
        return Paths.get(CONFIG_DIR, CONFIG_FILE);
    }
}
