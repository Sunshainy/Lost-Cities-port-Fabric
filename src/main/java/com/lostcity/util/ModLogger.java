package com.lostcity.util;

import com.lostcity.LostCityMod;

/**
 * Централизованная система логирования для мода
 * Упрощает отладку и отслеживание этапов генерации
 * 
 * DEBUG MODE: установите системное свойство -Dlostcity.debug=true для включения debug логов
 */
public final class ModLogger {

    private static final boolean DEBUG_MODE = Boolean.getBoolean("lostcity.debug");
    
    private ModLogger() {}
    
    /** Проверка, включен ли debug режим */
    public static boolean isDebugEnabled() {
        return DEBUG_MODE;
    }

    /** Логирование старта инициализации */
    public static void logInitStart() {
        LostCityMod.LOGGER.info("================================================");
        LostCityMod.LOGGER.info("  {} v{} for Minecraft {} - Initializing",
                LostCityMod.MOD_NAME,
                LostCityMod.VERSION,
                LostCityMod.MINECRAFT_VERSION);
        LostCityMod.LOGGER.info("  Fabric Port by sunshainy");
        LostCityMod.LOGGER.info("  Original Forge mod by McJty");
        if (DEBUG_MODE) {
            LostCityMod.LOGGER.info("  [DEBUG MODE ENABLED]");
        }
        LostCityMod.LOGGER.info("================================================");
    }

    /** Логирование завершения инициализации */
    public static void logInitComplete() {
        LostCityMod.LOGGER.info("================================================");
        LostCityMod.LOGGER.info("  {} - Initialization COMPLETE!", LostCityMod.MOD_NAME);
        LostCityMod.LOGGER.info("================================================");
    }

    /** Логирование старта клиентской инициализации */
    public static void logClientInitStart() {
        LostCityMod.LOGGER.info("[CLIENT] Initializing client-side components...");
    }

    /** Логирование завершения клиентской инициализации */
    public static void logClientInitComplete() {
        LostCityMod.LOGGER.info("[CLIENT] Client initialization complete!");
    }

    /** DEBUG логирование (только если DEBUG_MODE включен) */
    public static void debug(String message, Object... args) {
        if (DEBUG_MODE) {
            LostCityMod.LOGGER.info("[DEBUG] " + message, args);
        }
    }

    /** INFO логирование (всегда включено, основная информация) */
    public static void info(String message, Object... args) {
        LostCityMod.LOGGER.info(message, args);
    }

    /** WARNING логирование (всегда включено) */
    public static void warn(String message, Object... args) {
        LostCityMod.LOGGER.warn(message, args);
    }

    /** ERROR логирование (всегда включено) */
    public static void error(String message, Object... args) {
        LostCityMod.LOGGER.error(message, args);
    }
}
